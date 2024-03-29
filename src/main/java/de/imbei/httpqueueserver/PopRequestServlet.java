package de.imbei.httpqueueserver;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the queue of relay requests to the polling module.
 * A POST to /pop-request will return the relay request, packaged as JSON.
 * The query parameter "w" specifies the time, that the request is delayed if 
 * there is no request in the queue.
 */
@WebServlet(name = "PopRequestServlet", urlPatterns = {"/pop-request"})
public class PopRequestServlet extends HttpServlet {
    
    private RequestManager requestManger = RequestManager.getInstance();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        String waitingTimeParam = request.getParameter("w");
        int waitingTime;
        if (waitingTimeParam == null) {
            waitingTime = 30;
        } else{
            waitingTime = Integer.parseInt(waitingTimeParam);
        }
        
        try {
            RequestData requestData = requestManger.popRequest(waitingTime);
            if (requestData != null) {
                response.getWriter().print(requestData.toString());
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(PopRequestServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Returns new requests from the queue";
    }// </editor-fold>

}
