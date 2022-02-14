package de.imbei.httprelayserver;

import com.google.gson.Gson;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author lenzstef
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
