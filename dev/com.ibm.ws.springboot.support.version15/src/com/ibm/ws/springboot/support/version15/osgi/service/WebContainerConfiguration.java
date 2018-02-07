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
package com.ibm.ws.springboot.support.version15.osgi.service;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.app.manager.springboot.container.SpringContainer;

/**
 *
 */
public final class WebContainerConfiguration {
    public static WebContainerConfiguration getWebContainerConfiguration(Object consumer) {
        // A Spring Container is registered with the gateway bundle for the application
        // here we find the gateway bundle by looking for a BundleReference in the class
        // loader hierarchy.

        ClassLoader cl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> consumer.getClass().getClassLoader());
        while (cl != null && (!(cl instanceof BundleReference))) {
            cl = cl.getParent();
        }
        if (cl == null) {
            throw new IllegalStateException("Did not find a BundleReference class loader.");
        }

        return getWebContainerConfiguration(cl);
    }

    /**
     * @param appService
     * @return
     */
    private static synchronized WebContainerConfiguration getWebContainerConfiguration(ClassLoader cl) {
        WebContainerConfiguration existing = getAppService(cl, WebContainerConfiguration.class);
        if (existing != null) {
            return existing;
        }
        Bundle b = ((BundleReference) cl).getBundle();
        SpringContainer springContainer = getAppService(cl, SpringContainer.class);
        if (springContainer == null) {
            throw new IllegalStateException("No Spring Container service found for: " + b);
        }
        WebContainerConfiguration result = new WebContainerConfiguration(getAppService(cl, SpringContainer.class));
        b.getBundleContext().registerService(WebContainerConfiguration.class, result, null);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getAppService(ClassLoader cl, Class<T> type) {
        Bundle b = ((BundleReference) cl).getBundle();
        ServiceReference<?>[] services = b.getRegisteredServices();
        if (services != null) {
            for (ServiceReference<?> service : services) {
                String[] objectClass = (String[]) service.getProperty(Constants.OBJECTCLASS);
                for (String name : objectClass) {
                    if (type.getName().equals(name)) {
                        return (T) b.getBundleContext().getService(service);
                    }
                }
            }
        }
        return null;
    }

    private final SpringContainer springContainer;
    private final AtomicReference<UnaryOperator<ServletContext>> contextListener = new AtomicReference<>();

    /**
     * @param springContainer
     * @param cl
     */
    private WebContainerConfiguration(SpringContainer springContainer) {
        this.springContainer = springContainer;
    }

    public void deploy(UnaryOperator<ServletContext> contextListener) {
        this.contextListener.set(contextListener);
        // Deploy the application will make the application known to the
        // web container.
        springContainer.deploy();
    }

    public UnaryOperator<ServletContext> getListener() {
        return contextListener.get();
    }
}
