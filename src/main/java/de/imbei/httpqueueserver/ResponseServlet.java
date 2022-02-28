package de.imbei.httpqueueserver;

import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The /response endpoint accepts the answers for requests.
 */
public class ResponseServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String requestBody = RequestData.extractBody(request);
        Gson gson = new Gson();
        ResponseData responseData = gson.fromJson(requestBody, ResponseData.class);
        RequestManager.registerResponse(responseData);
    }


    @Override
    public String getServletInfo() {
        return "Registers resposes for queued requests";
    }

}
