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
package com.ibm.ws.springboot.support.version15.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = {})
public class Activator implements BundleActivator {

    @Override
    @Activate
    public void start(BundleContext bc) throws Exception {
        System.out.println("GET RID OF THIS ACTIVATOR: " + bc.getBundle());
    }

    @Override
    @Deactivate
    public void stop(BundleContext arg0) throws Exception {}

}
