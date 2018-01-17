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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.ApplicationManager;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.kernel.service.location.WsResource;

@Component(service = DeployedAppInfoFactory.class,
           property = { "service.vendor=IBM", "type=" + SPRING_APP_TYPE })
public class SpringDeployedAppInfoFactoryImpl extends DeployedAppInfoFactoryBase {

    private static final TraceComponent tc = Tr.register(SpringDeployedAppInfoFactoryImpl.class);

    protected ModuleHandler springModuleHandler;
    private ApplicationManager applicationManager;
    private final ZipUtils zipUtils = new ZipUtils();
    private final static Map<String, Long> timestamps = new HashMap<String, Long>();

    @Reference(target = "(type=" + SPRING_APP_TYPE + ")")
    protected void setWebModuleHandler(ModuleHandler handler) {
        springModuleHandler = handler;
    }

    protected void unsetWebModuleHandler(ModuleHandler handler) {
        springModuleHandler = null;
    }

    @Reference
    protected void setApplicationManager(ApplicationManager mgr) {
        this.applicationManager = mgr;
    }

    protected void unsetApplicationManager(ApplicationManager mgr) {
        this.applicationManager = null;
    }

    @Override
    public SpringDeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation) throws UnableToAdaptException {

        try {
            // Check whether we need to expand this WAR
            if (applicationManager.getExpandApps()) {

                // Make sure this is a file and not an expanded directory
                String location = applicationInformation.getLocation();
                File springAppFile = new File(location);
                if (springAppFile.isFile() && !location.toLowerCase().endsWith(XML_SUFFIX)) {

                    // Make sure the apps/expanded directory is available
                    WsResource expandedAppsDir = getLocationAdmin().resolveResource(EXPANDED_APPS_DIR);
                    expandedAppsDir.create();

                    // Store the spring app file timestamp and get the current value (if it exists)
                    Long springFileTimestamp = timestamps.put(springAppFile.getAbsolutePath(), springAppFile.lastModified());

                    WsResource expandedSpringDir = getLocationAdmin().resolveResource(EXPANDED_APPS_DIR + applicationInformation.getName() + "." + SPRING_APP_TYPE + "/");
                    if (expandedSpringDir.exists()) {
                        // If the expanded Spring app directory already exists, we need to try to figure out if this was an update to the Spring App file in apps/dropins
                        // or an update to the expanded directory. We do this by checking the Spring app file timestamp against a stored value.
                        //
                        // Doing it this way is really unfortunate, but it seems to be the best option at the moment.
                        // TODO - Either figure out a legitimate way to use Notifier to determine which container changed, or
                        // improve by caching the timestamp values

                        // If we don't have a timestamp for the Spring app, or the Spring app file has been changed, delete the expanded directory
                        if (springFileTimestamp == null || springFileTimestamp.longValue() != springAppFile.lastModified()) {
                            zipUtils.recursiveDelete(expandedSpringDir.asFile());

                            // Create the expanded directory again
                            expandedSpringDir.create();

                            // Unzip the Spring app into the expanded directory
                            zipUtils.unzip(springAppFile, expandedSpringDir.asFile());
                        }
                    } else {
                        // The expanded directory doesn't exist yet, so create it and unzip the Spring app file contents into it
                        expandedSpringDir.create();
                        zipUtils.unzip(springAppFile, expandedSpringDir.asFile());
                    }

                    // Set up the new container pointing to the expanded directory
                    Container container = setupContainer(applicationInformation.getPid(), expandedSpringDir.asFile());
                    applicationInformation.setContainer(container);

                }

            }
        } catch (IOException ex) {
            // Log error and continue to use the container for the SPR file
            Tr.error(tc, "warning.could.not.expand.application", applicationInformation.getName(), ex.getMessage());
        }

        SpringDeployedAppInfo deployedApp = new SpringDeployedAppInfo(applicationInformation, this);
        applicationInformation.setHandlerInfo(deployedApp);
        return deployedApp;
    }

}
