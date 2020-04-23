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
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.wab.configure.WABConfiguration;

@WebServlet("/switch")
public class Switch extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private ServiceRegistration<WABConfiguration> configuration;

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Bundle thisBundle = FrameworkUtil.getBundle(this.getClass());
        BundleContext bc = thisBundle.getBundleContext();

        try {
            if (configuration != null) {
                configuration.unregister();
            }
            String context = request.getParameter("context");
            Hashtable<String, String> props = new Hashtable<>();
            props.put(WABConfiguration.CONTEXT_NAME, context);
            props.put(WABConfiguration.CONTEXT_PATH, "/switchTarget");
            configuration = bc.registerService(WABConfiguration.class, new WABConfiguration() {
            }, props);
            response.getOutputStream().println("SUCCESS service: " + getClass().getName());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            response.getOutputStream().println("FAIL service: " + getClass().getName() + " " + e.getMessage());
        }
    }

}
