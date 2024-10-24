/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package jaxrs21.fat.form;

import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/FormBehaviorTestServlet")
public class FormBehaviorTestServlet extends FATServlet {

    private Client client;

    
    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @Override
    public void destroy() {
        client.close();
    }

    @Test
    public void testInterceptorInvokedOnFormAndFormParamMatchesFormValue(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        
        String uri = "http://localhost:" + req.getServerPort() + "/formApp/form";
        Form f = new Form("value", "ORIGINAL");
        Response r = client.target(uri)
                           .request(MediaType.APPLICATION_FORM_URLENCODED)
                           .header("REPLACE-STREAM", "true")
                           .post(Entity.form(f));
        assertEquals("MODIFIED", r.getHeaderString("FromFormParam"));
        assertEquals("MODIFIED", r.getHeaderString("FromForm"));
    }

    @Test
    public void testCRLFsArePreserved(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        
        String uri = "http://localhost:" + req.getServerPort() + "/formApp/form/crlf";
        String text = "Line 1" + System.lineSeparator() + "Line 2" + System.lineSeparator() + "Line 3";
        Response r = client.target(uri)
                           .request(MediaType.TEXT_PLAIN + "; charset=utf-8")
                           .header("REPLACE-STREAM", "false")
                           .post(Entity.text(text));
        assertEquals(200, r.getStatus());
    }
}