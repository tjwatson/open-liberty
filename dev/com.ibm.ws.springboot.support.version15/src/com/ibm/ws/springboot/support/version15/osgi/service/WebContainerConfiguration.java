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

import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.app.manager.springboot.container.SpringContainer;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 *
 */
public final class WebContainerConfiguration {
    public static WebContainerConfiguration getWebContainerConfiguration(Object consumer) {
        ClassLoader cl = consumer.getClass().getClassLoader();
        while (cl != null && (!(cl instanceof BundleReference))) {
            cl = cl.getParent();
        }
        if (cl == null) {
            throw new IllegalStateException("Did not find a BundleReference class loader.");
        }
        return new WebContainerConfiguration(getSpringContainer(cl), cl);
    }

    private static SpringContainer getSpringContainer(ClassLoader cl) {
        Bundle b = ((BundleReference) cl).getBundle();
        ServiceReference<?>[] services = b.getRegisteredServices();
        if (services != null) {
            for (ServiceReference<?> service : services) {
                String[] objectClass = (String[]) service.getProperty(Constants.OBJECTCLASS);
                for (String name : objectClass) {
                    if (SpringContainer.class.getName().equals(name)) {
                        return (SpringContainer) b.getBundleContext().getService(service);
                    }
                }
            }
        }
        throw new IllegalStateException("No SpringContainer found for bundle: " + b);
    }

    private final SpringContainer springContainer;
    private final ClassLoader cl;

    /**
     * @param springContainer
     * @param cl
     */
    private WebContainerConfiguration(SpringContainer springContainer, ClassLoader cl) {
        this.springContainer = springContainer;
        this.cl = cl;
    }

    public void deploy(UnaryOperator<ServletContext> contextListener) {
        registerExtensionFactory(contextListener);
        springContainer.deploy();
    }

    /**
     * @return
     */
    private void registerExtensionFactory(final UnaryOperator<ServletContext> contextListener) {
        ServiceFactory<ExtensionFactory> serviceFactory = new ServiceFactory<ExtensionFactory>() {
            @Override
            public ExtensionFactory getService(Bundle b, final ServiceRegistration<ExtensionFactory> reg) {
                return new ExtensionFactory() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public List getPatternList() {
                        return Collections.emptyList();
                    }

                    @Override
                    public ExtensionProcessor createExtensionProcessor(IServletContext webapp) throws Exception {
                        // TODO figure out how to make sure this is the correct context
                        if (true /* webapp.getClassLoader() == cl */) {
                            contextListener.apply(webapp);
                            reg.unregister();
                        }
                        return null;
                    }
                };
            }

            @Override
            public void ungetService(Bundle b, ServiceRegistration<ExtensionFactory> reg, ExtensionFactory service) {
                // nothing
            }

        };
        FrameworkUtil.getBundle(getClass()).getBundleContext().registerService(ExtensionFactory.class, serviceFactory, null);
    }
}
