/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.ibm.ws.jca.fat.classloading.cdilib.CDIBean;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/*")
public class CDITestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private CDIBean cdiBean;

    /**
     * Test that CDI bean from library can be injected and used.
     *
     * @param request HTTP request
     * @param out     writer for the HTTP response
     * @throws Exception if an error occurs.
     */
    @Test
    public void testCDIBeanFromLibrary() throws Exception {

        assertNotNull("CDI Bean was not injected", cdiBean);
        assertEquals(CDIBean.MESSAGE, cdiBean.getMessage());

    }
}
