/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.imbei.httpqueueserver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the data from the response of the target server plus the request ID.
 */
public class ResponseData {
    
    private int requestId;
    private int statusCode;
    private Map<String, List<String>> headers;
    private String body;
    
    public static ResponseData createTimeoutResponse(int requestId) {
        ResponseData timeoutResponse = new ResponseData();
        timeoutResponse.requestId = requestId;
        timeoutResponse.statusCode = 504; // Gateway timeout
        timeoutResponse.headers = new HashMap<>();
        timeoutResponse.setBody("Timeout while processing request via polling.");
        timeoutResponse.headers
                .put("Content-Type", Collections.singletonList("text/plain; charset=UTF-8"));
        return timeoutResponse;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
    
    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

}
