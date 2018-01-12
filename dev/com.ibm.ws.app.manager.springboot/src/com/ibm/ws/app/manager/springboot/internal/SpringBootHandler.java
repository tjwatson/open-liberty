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
package com.ibm.ws.app.manager.springboot.internal;

import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;

@Component(property = { "service.vendor=IBM", "type=spr" })
public class SpringBootHandler implements ApplicationHandler<DeployedAppInfo> {

    @Override
    public Future<Boolean> install(ApplicationInformation<DeployedAppInfo> appInfo) {
        return null;
    }

    @Override
    public ApplicationMonitoringInformation setUpApplicationMonitoring(ApplicationInformation<DeployedAppInfo> appInfo) {
        return null;
    }

    @Override
    public Future<Boolean> uninstall(ApplicationInformation<DeployedAppInfo> appInfo) {
        return null;
    }

}
