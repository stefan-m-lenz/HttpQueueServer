package de.imbei.relayserver;

import java.util.LinkedList;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author lenzstef
 */
public class RequestManager {
    
    private static int requestCounter = 0;
    private static final LinkedList<RequestData> requestQueue = new LinkedList<>();
       
    private synchronized static void storeRequest(HttpServletRequest request) {
        requestQueue.add(new RequestData(request, newRequestId()));
    }
    
    private synchronized static int newRequestId() {
        requestCounter += 1;
        if (requestCounter < 0) {
            requestCounter = 0; // overflow, use only positive values
        }
        return requestCounter;
    }
    
    /*
     *
     */
    public static void relayRequest(HttpServletRequest request, HttpServletResponse response) {
        storeRequest(request);
        // wait for answer 
        // write answer
    }
    
    public synchronized static RequestData popRequest() {
        if (requestQueue.isEmpty()) {
            return null;
        } else {
            return requestQueue.removeFirst();
        }
    }
    
    public synchronized static void registerAnswer(HttpServletRequest reqeust) {
        
    }
}
