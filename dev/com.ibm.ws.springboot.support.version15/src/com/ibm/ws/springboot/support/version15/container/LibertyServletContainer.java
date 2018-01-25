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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import com.ibm.ws.springboot.support.version15.osgi.service.WebContainerConfiguration;

/**
 *
 */
public class LibertyServletContainer implements EmbeddedServletContainer {
    private final ServletContextInitializer[] initializers;

    /**
     * @param mergeInitializers
     */
    public LibertyServletContainer(ServletContextInitializer[] initializers) {
        this.initializers = initializers;
        start0();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.boot.context.embedded.EmbeddedServletContainer#getPort()
     */
    @Override
    public int getPort() {
        // TODO get real port
        return 9080;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.boot.context.embedded.EmbeddedServletContainer#start()
     */
    private void start0() throws EmbeddedServletContainerException {
        final CountDownLatch initDone = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        WebContainerConfiguration.getWebContainerConfiguration(this).deploy((sc) -> {
            try {
                for (ServletContextInitializer servletContextInitializer : initializers) {
                    try {
                        servletContextInitializer.onStartup(sc);
                    } catch (Throwable t) {
                        exception.set(t);
                        break;
                    }
                }
            } finally {
                initDone.countDown();
            }
            return sc;
        });
        try {
            initDone.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddedServletContainerException("Initialization of ServletContext got interrupted.", e);
        }
        if (exception.get() != null) {
            throw new EmbeddedServletContainerException("Error occured initializing the ServletContext.", exception.get());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.boot.context.embedded.EmbeddedServletContainer#start()
     */
    @Override
    public void start() throws EmbeddedServletContainerException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.boot.context.embedded.EmbeddedServletContainer#stop()
     */
    @Override
    public void stop() throws EmbeddedServletContainerException {
        // TODO Auto-generated method stub

    }

}
