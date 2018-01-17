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

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_APP_TYPE;

import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;

@Component(property = { "service.vendor=IBM", "type=" + SPRING_APP_TYPE })
public class SpringBootHandler implements ApplicationHandler<DeployedAppInfo> {

    private FutureMonitor futureMonitor;
    private DeployedAppInfoFactory deployedAppFactory;

    @Reference
    protected void setFutureMonitor(FutureMonitor fm) {
        futureMonitor = fm;
    }

    @Reference(target = "(type=" + SPRING_APP_TYPE + ")")
    protected void setDeployedAppFactory(DeployedAppInfoFactory factory) {
        deployedAppFactory = factory;
    }

    @Override
    public ApplicationMonitoringInformation setUpApplicationMonitoring(ApplicationInformation<DeployedAppInfo> appInfo) {
        Container oldContainer = appInfo.getContainer();

        final SpringDeployedAppInfo deployedApp;
        try {
            deployedApp = (SpringDeployedAppInfo) deployedAppFactory.createDeployedAppInfo(appInfo);
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }

        return deployedApp.createApplicationMonitoringInformation(oldContainer);

    }

    @Override
    public Future<Boolean> install(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        final Future<Boolean> result = futureMonitor.createFuture(Boolean.class);

        SpringDeployedAppInfo deployedApp = (SpringDeployedAppInfo) applicationInformation.getHandlerInfo();

        if (!deployedApp.deployApp(result)) {
            futureMonitor.setResult(result, false);
            return result;
        }

        return result;
    }

    @Override
    public Future<Boolean> uninstall(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        SpringDeployedAppInfo deployedApp = (SpringDeployedAppInfo) applicationInformation.getHandlerInfo();
        if (deployedApp == null) {
            // Somebody asked us to remove an app we don't know about
            return futureMonitor.createFutureWithResult(false);
        }

        boolean success = deployedApp.uninstallApp();
        return futureMonitor.createFutureWithResult(success);
    }
}
