package de.imbei.httprelayserver;

import java.util.LinkedList;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author lenzstef
 */
public class RequestManager {
    
    private static int requestCounter = 0;
    private static final LinkedList<RequestData> requestQueue = new LinkedList<>();
    private static final Map<Integer, ResponseData> responses = Collections.synchronizedMap(new HashMap<>());
    
    // make requestCounter thread safe
    private static final Lock requestCounterLock = new ReentrantLock();
    
    //make requestQueue thread safe
    private static final Lock requestQueueLock = new ReentrantLock();
   
    
    // make it possible to wait for a response and be notified upon arrival
    private static final Map<Integer, Lock> responseLocks = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Condition> responseConditions = Collections.synchronizedMap(new HashMap<>());
            

    
    private static void queueRequest(HttpServletRequest request, int requestId) {
        requestQueueLock.lock();
        try {
            requestQueue.add(new RequestData(request, requestId));    
        } finally {
            requestQueueLock.unlock();
        }
    }
    
    private static int newRequestId() {
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
    
    /*
     *
     */
    public static void relayRequest(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, IOException {
        int requestId = newRequestId();
        
        queueRequest(request, requestId);
        
        waitForResponse(requestId);
        
        ResponseData responseData = responses.get(requestId);
        responses.remove(requestId);

        writeResponse(response, responseData);
    }
    
    private static void waitForResponse(int requestId) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        responseLocks.put(requestId, lock);
        lock.lock();
        try {
            Condition responseArrived = lock.newCondition();
            responseConditions.put(requestId, responseArrived);
            while (responses.get(requestId) == null) {
                responseArrived.await();
            }
            
        } finally {
            lock.unlock();
        }
        responseLocks.remove(requestId);
        responseConditions.remove(requestId);
    }
    
    private static void writeResponse(HttpServletResponse response, ResponseData responseData) throws IOException {
        response.setStatus(responseData.getStatusCode());
        
        // set headers
        for (Entry<String, List<String>> kv: responseData.getHeaders().entrySet()) {
            for (String value : kv.getValue()) {
                response.addHeader(kv.getKey(), value);
            }
        }
        
        //set body
        response.getWriter().print(responseData.getBody());
    }
    
    public static RequestData popRequest() {
        requestQueueLock.lock();
        try {
            if (requestQueue.isEmpty()) {
                return null;
            } else {
                return requestQueue.removeFirst();
            }
        } finally {
            requestQueueLock.unlock();
        }
    }

    
    public static void registerResponse(ResponseData responseData) {
        int requestId = responseData.getRequestId();
        Lock responseLock = responseLocks.get(requestId);
        responseLock.lock();
        try {
            responses.put(requestId, responseData);
            // wake up thread that waits on response after registering it
            responseConditions.get(requestId).signal();
        } finally {
            responseLock.unlock();
        }
    }
}
