/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.version15.container;

import java.util.function.UnaryOperator;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.ibm.ws.springboot.support.version15.osgi.service.WebContainerConfiguration;

@WebListener
public class LibertyContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        UnaryOperator<ServletContext> listener = WebContainerConfiguration.getWebContainerConfiguration(this).getListener();
        listener.apply(event.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {

    }

}
