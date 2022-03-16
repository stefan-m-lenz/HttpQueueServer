package de.imbei.httpqueueserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

/**
 * This class stores all information from a HttpServletRequest and attaches an
 * ID.
 */
public class RequestData {

    /**
     * These HTTP headers cannot be set in the HttpClient. They can be ignored
     * when assembling the request data. The values are extracted from:
     * https://github.com/openjdk/jdk/blob/47b1c51bbd28582d209db07052e553a76acced65/src/java.net.http/share/classes/jdk/internal/net/http/common/Utils.java#L136
     */
    private static final Set<String> RESTRICTED_HEADERS
            = Set.of("connection", "content-length", "expect", "host", "upgrade");
    
    private final int requestId;
    private final String method;
    private final String uri;
    private final Map<String, List<String>> headers;
    private final String body;

    /**
     * Extracts "abc/xyz?v=2" from a request to
     * "https://queue.example.com/relay/abc/xyz?v=2".
     *
     * @param request the original request
     * @return the extracted part of the URI that is to be passed to the polling
     * module
     */
    private static String extractUri(HttpServletRequest request) {
        int contextPathChars = request.getContextPath().length()
                + request.getServletPath().length() + 1;
        String uri = request.getRequestURI();
        if (uri.length() < contextPathChars) {
            uri = ""; // request to /relay without / at the end
        } else {
            uri = uri.substring(contextPathChars);
        }
        String queryString = request.getQueryString();
        if (queryString == null) {
            queryString = "";
        } else {
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
                if (!RESTRICTED_HEADERS.contains(nextHeaderName)) {
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
        }
        return headers;
    }

    public static String extractBody(HttpServletRequest request) {
        String requestBody = "";
        if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
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
        this.body = extractBody(request);
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(this);
    }

}
