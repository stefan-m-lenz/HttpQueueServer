package de.imbei.httprelayserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
 * This class stores all information from a HttpServletRequest
 * and attaches an ID.
 */
public class RequestData {
    
    private final int requestId;
    private final String method;
    private final String uri;
    private final Map<String, List<String>> headers;
    private final String body;
    
    
    private static String extractUri(HttpServletRequest request) {
        int contextPathChars = request.getContextPath().length() + 
                request.getServletPath().length() + 1;
        String uri = request.getRequestURI();
        if (uri.length() < contextPathChars) {
            uri = ""; // request to /relay without / at the end
        } else {
            uri = uri.substring(contextPathChars);
        }
        String queryString = request.getQueryString();
        if (queryString == null) {
            queryString = "";
        }
        else {
            queryString = "?" + queryString;
        }
        return uri + queryString;
    }
    
    
    private static Map<String, List<String>> extractHeaders(HttpServletRequest request) {
        HashMap<String, List<String>> headers = new HashMap<>();
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
        return headers;
    }
    
    
    private static String extractBody(HttpServletRequest request, String method) {
        String requestBody = "";
        if ("POST".equals(method)) {
            try {
                requestBody = request.getReader().lines()
                        .collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException ex) {
                Logger.getLogger(RequestData.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return requestBody;
    }
    
    public RequestData(HttpServletRequest request, int requestId) {
        this.requestId = requestId;
        
        this.method = request.getMethod();        
        this.uri = extractUri(request);
        this.headers = extractHeaders(request);
        this.body = extractBody(request, this.method);
    }
    
    @Override
    public String toString() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(this);
    }
    
}
