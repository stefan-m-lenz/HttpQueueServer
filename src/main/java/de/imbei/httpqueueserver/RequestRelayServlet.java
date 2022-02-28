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
 * Relayss requests to the target server.
 * Requests of the form /relay/abc/xyz?u=123 are translated to requests like
 * http://the-target-server-url/abc/xyz?u=123 and the answer is returned as
 * answer of this servlet.
 * This way, the response looks like it is coming from the target server.
 */
@WebServlet(name = "RequestRelayServlet", urlPatterns = {"/relay"})
public class RequestRelayServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            RequestManager.relayRequest(request, response);
        } catch (InterruptedException ex) {
            Logger.getLogger(RequestRelayServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
 
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Relay HTTP requests via polling";
    }// </editor-fold>

}
