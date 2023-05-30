package de.imbei.httpqueueserver;

import java.util.LinkedList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the request queue and the incoming responses.
 */
public class RequestManager {
       
    private int requestCounter = 0;
    private final LinkedList<RequestData> requestQueue = new LinkedList<>();
    private final Map<Integer, ResponseData> responses = Collections.synchronizedMap(new HashMap<>());
      
    // make requestCounter thread safe
    private final Lock requestCounterLock = new ReentrantLock();
    
    // make requestQueue thread safe
    private final Lock requestQueueLock = new ReentrantLock();
    // let the polling module wait for this condition:
    private final Condition requestArrived = requestQueueLock.newCondition();
    
    // make it possible to wait for a response and be notified upon arrival
    private final Map<Integer, Lock> responseLocks = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, Condition> responseConditions = Collections.synchronizedMap(new HashMap<>());
    
    // The time of each request is stored until the response has been delivered.
    // This way, a clean-up task can remove requests that have timed out,
    // as well as corresponding responses that may never be fetched
    // because the request has timed out already.
    private final Map<Integer, Long> requestTimes = new HashMap<>();
    
    // make requestTimes thread safe
    private final Lock requestTimesLock = new ReentrantLock();
    
    private long timeoutMillis = 2000; // TODO make configurable, sensible value

    // This class follows the singleton pattern.
    private static volatile RequestManager instance;
    
    private RequestManager() {}

    public static RequestManager getInstance() {
        if (instance == null) {
            synchronized (RequestManager.class) {
                if (instance == null) {
                    instance = new RequestManager();
                }
            }
        }
        return instance;
    }

    
    private void queueRequest(HttpServletRequest request, int requestId) {
        // Register the request time before adding it to the queue.
        requestTimesLock.lock();
        try {
            requestTimes.put(requestId, System.currentTimeMillis());
        } finally {
            requestTimesLock.unlock();
        }
        
        requestQueueLock.lock();
        try {
            requestQueue.add(new RequestData(request, requestId));
            requestArrived.signal();
        } finally {
            requestQueueLock.unlock();
        }
    }
    
    private int newRequestId() {
        requestCounterLock.lock();
        try {
            requestCounter += 1;
            if (requestCounter < 0) {
                requestCounter = 0; // overflow, use only positive values
            }
        } finally {
            requestCounterLock.unlock();
        }
        return requestCounter;
    }
    
    
    public void relayRequest(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, IOException {
        int requestId = newRequestId();
        
        queueRequest(request, requestId);
        
        // TODO catch timeout
        ResponseData responseData = waitForResponse(requestId);

        writeResponse(response, responseData);
    }
    
    private ResponseData waitForResponse(int requestId) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        responseLocks.put(requestId, lock);
        lock.lock();
        try {
            Condition responseArrived = lock.newCondition();
            responseConditions.put(requestId, responseArrived);
            
            // TODO timeout
            while (responses.get(requestId) == null) {
                responseArrived.await();
            }
            
            return responses.get(requestId);
        } finally {           
            lock.unlock();
            
            responses.remove(requestId);
            responseLocks.remove(requestId);
            responseConditions.remove(requestId);
            
            // After removing the response, the requestTimes object can
            // also be removed, because the request does not have to be tracked 
            // any more.
            removeRequestTime(requestId);
        }
    }
    
    private Long getRequestTime(Integer requestId) {
        requestTimesLock.lock();
        try {
            return requestTimes.get(requestId);
        } finally {
            requestTimesLock.unlock();
        }        
    }
    
    private void removeRequestTime(Integer requestId) {
        requestTimesLock.lock();
        try {
            requestTimes.remove(requestId);
        } finally {
            requestTimesLock.unlock();
        }        
    }
    
    private void writeResponse(HttpServletResponse response, ResponseData responseData) throws IOException {
        response.setStatus(responseData.getStatusCode());
        
        // set headers
        for (Entry<String, List<String>> kv: responseData.getHeaders().entrySet()) {
            for (String value : kv.getValue()) {
                response.addHeader(kv.getKey(), value);
            }
        }
        
        //set body
        byte[] body = Base64.getDecoder().decode(responseData.getBody());
        response.getOutputStream().write(body);
    }
    
    // Get new request. Wait until waitingTime is over or until a new request arrives
    public RequestData popRequest(int waitingTime) throws InterruptedException {
        requestQueueLock.lock();
        try {
            long timeStart = System.currentTimeMillis();
            while (requestQueue.isEmpty()) {
                requestArrived.await(waitingTime, TimeUnit.SECONDS);
                long elapsedMillis = System.currentTimeMillis() - timeStart;
                if (elapsedMillis > waitingTime * 1000) {
                    break;
                }
            }
            if (requestQueue.isEmpty()) {
                return null;
            } else {
                return requestQueue.removeFirst();
            }
        } finally {
            requestQueueLock.unlock();
        }
    }

    
    public void registerResponse(ResponseData responseData) {
        int requestId = responseData.getRequestId();
        Lock responseLock = responseLocks.get(requestId);
        responseLock.lock();
        try {
            // If there is a responseTimes object for this request,
            // this means, that a thread is still waiting for the reponse.
            // Register the response and wake up the thread that waits for it.
            if (getRequestTime(requestId) != null) {
                responseConditions.get(requestId).signal();
                responses.put(requestId, responseData);
            }
            // Otherwise, the request has already been cleaned up due to a timeout
            // and no one is waiting for it. Do nothing and discard the response.
        } finally {
            responseLock.unlock();
        }
    }
    
    
    // Clean up requests, responses and corresponding objects 
    // for timed out requests
    public void cleanUp() {
        List<Integer> expiredRequests = getExpiredRequests();
        for (Integer requestId : expiredRequests) {
            if (responses.get(requestId) != null) {
                // If a response has been registered for the requestId
                // but no thread has fetched it, 
                // clean up the response object and the corresponding locks.
                // (The object can't be in the queue any more because the
                // response has been registered. No need to remove it there.)
                responses.remove(requestId);
                responseLocks.remove(requestId);
                responseConditions.remove(requestId);
                removeFromRequestTimes(requestId);
            } else {
                // If the timeout is due to the fact that the request has not been
                // fetched from the queue, remove the request from the queue.
                if (!removeRequestFromQueue(requestId)) {
                    // If it's not in the queue, 
                    // and there is no response object registered, 
                    // we have a timeout of the request. However, it is possible
                    // that the client is still waiting for the request.
                    // To make the timeout visible to a client that may still be waiting,
                    // register a timeout response.
                    registerResponse(ResponseData.createTimeoutResponse(requestId));
                    // (If this response is not fetched, it will be cleaned up 
                    // when the clean-up task runs for the next time.)
                }
            }
        }
    }
    
    // Removes a request from the queue and returns true if the request was 
    // in the queue before removing it.
    private boolean removeRequestFromQueue(int requestId) {
        requestQueueLock.lock();
        try {
            return requestQueue.removeIf(request -> request.getRequestId() == requestId);
        } finally {
            requestQueueLock.unlock();
        }
    }
    
    // Removes a requestTimes entry for a given request ID
    private void removeFromRequestTimes(int requestId) {
        requestTimesLock.lock();
        try {
            requestTimes.remove(requestId);
        } finally {
            requestTimesLock.unlock();
        }
    }
       
    // Find all expired requests in the requestTimes.
    // Returns a list of their request IDs
    private List<Integer> getExpiredRequests() {
        long currentTime = System.currentTimeMillis();
        requestTimesLock.lock();
        try {
            List<Integer> expiredRequests = requestTimes.entrySet().stream()
                    .filter(e -> e.getValue() + timeoutMillis > currentTime)
                    .map(Entry::getKey) // collect request IDs
                    .collect(Collectors.toList());
            return expiredRequests;
        } finally {
            requestTimesLock.unlock();
        }
    }
}
