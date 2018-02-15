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

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import com.ibm.ws.app.manager.springboot.container.config.HttpEndpoint;
import com.ibm.ws.app.manager.springboot.container.config.HttpSession;
import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.VirtualHost;
import com.ibm.ws.springboot.support.version15.osgi.service.WebContainerConfiguration;

/**
 *
 */
public class LibertyServletContainer implements EmbeddedServletContainer {
    private final ServletContextInitializer[] initializers;
    private final LibertyServletContainerFactory factory;

    /**
     * @param libertyServletContainerFactory
     * @param mergeInitializers
     */
    public LibertyServletContainer(LibertyServletContainerFactory factory, ServletContextInitializer[] initializers) {
        this.initializers = initializers;
        this.factory = factory;
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
        WebContainerConfiguration containerConfig = WebContainerConfiguration.getWebContainerConfiguration(this);
        ServerConfiguration serverConfig = containerConfig.createServerConfiguration();
        List<HttpEndpoint> endpoints = serverConfig.getHttpEndpoints();
        endpoints.clear();
        HttpEndpoint endpoint = new HttpEndpoint();
        if (factory.getAddress() != null) {
            endpoint.setHost(factory.getAddress().getHostAddress());
        } else {
            endpoint.setHost("0.0.0.0");
        }

        Ssl ssl = factory.getSsl();
        if (ssl == null || ssl.getKeyStore() == null) {
            endpoint.setHttpPort(factory.getPort());
            endpoint.setHttpsPort(-1);
            // TODO configure ssl
        } else {
            endpoint.setHttpPort(-1);
            endpoint.setHttpsPort(factory.getPort());
        }

        if (factory.getServerHeader() != null) {
            endpoint.getHttpOptions().setServerHeaderValue(factory.getServerHeader());
        }

        if (factory.getSessionTimeout() > 0) {
            configureSession(serverConfig);
        }
        endpoints.add(endpoint);

        List<VirtualHost> virtualHosts = serverConfig.getVirtualHosts();
        virtualHosts.clear();
        VirtualHost virtualHost = new VirtualHost();
        Set<String> aliases = virtualHost.getHostAliases();
        aliases.clear();
        // TODO would be better to use *:* for wildcarding the port
        aliases.add("*:" + factory.getPort());
        virtualHosts.add(virtualHost);

        containerConfig.configure(serverConfig);
    }

    /**
     * @param serverConfig
     */
    private void configureSession(ServerConfiguration serverConfig) {
        // TODO is this only configurable for all endpoints?
        HttpSession session = serverConfig.getHttpSession();
        session.setInvalidationTimeout(factory.getSessionTimeout());
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
