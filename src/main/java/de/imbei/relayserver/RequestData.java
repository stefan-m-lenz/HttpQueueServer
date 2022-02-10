package de.imbei.relayserver;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

/**
 *
 * @author lenzstef
 */
public class RequestData {
    
    private final int requestId;
    private final String method;
    private final String uri;
    private final Map<String, List<String>> headers;
    private final String body;
    
    public RequestData(HttpServletRequest request, int requestId) {
        this.requestId = requestId;
        
        //store method (GET/POST etc.)
        this.method = request.getMethod();
                
        // store uri
        this.uri = request.getPathInfo();
        
        // store headers
        this.headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String nextHeaderName = headerNames.nextElement();
                List<String> nextHeaderValue = headers.get(nextHeaderName);
                if (nextHeaderValue == null) {
                    nextHeaderValue = new ArrayList<>();
                    nextHeaderValue.add(request.getHeader(nextHeaderName));
                    headers.put(nextHeaderName, nextHeaderValue);
                } else {
                    nextHeaderValue.add(request.getHeader(nextHeaderName));
                }
            }
        }
        
        // store body
        String requestBody = "";
        if ("POST".equals(this.method)) {
            try {
                requestBody = request.getReader().lines()
                        .collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException ex) {
                Logger.getLogger(RequestData.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.body = requestBody;
    }
    
    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    
}
