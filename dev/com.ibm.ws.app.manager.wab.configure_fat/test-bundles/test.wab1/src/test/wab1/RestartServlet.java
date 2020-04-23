/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.wab1;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

@WebServlet("/restart")
public class RestartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Bundle thisBundle = FrameworkUtil.getBundle(this.getClass());
        BundleContext bc = thisBundle.getBundleContext();
        for (Bundle b : bc.getBundles()) {
            if ("test.wab3".equals(b.getSymbolicName())) {
                try {
                    b.stop();
                    b.start();
                    response.getOutputStream().println("SUCCESS service: " + getClass().getName());
                    return;
                } catch (BundleException e) {
                    e.printStackTrace();
                    response.getOutputStream().println("FAIL service: " + getClass().getName() + " " + e.getMessage());
                }
            }
        }
        response.getOutputStream().println("FAIL service: " + getClass().getName() + " no test.wab3 bundle found.");
    }

}
