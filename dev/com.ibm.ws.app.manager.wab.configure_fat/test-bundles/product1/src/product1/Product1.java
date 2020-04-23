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
package product1;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.wsspi.wab.configure.WABConfiguration;

@Component(configurationPid = "product1",
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Product1 implements WABConfiguration {
    // Just a marker service that uses configuration to set the 
    // contextName and contextPath service properties
}
