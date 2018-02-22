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
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_LIB_INDEX_FILE;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_THIN_APPS_DIR;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.springboot.support.SpringBootSupport;
import com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

@Component(service = DeployedAppInfoFactory.class,
           property = { "service.vendor=IBM", "type=" + SPRING_APP_TYPE })
public class SpringDeployedAppInfoFactoryImpl extends DeployedAppInfoFactoryBase {
    private static final TraceComponent tc = Tr.register(SpringDeployedAppInfoFactoryImpl.class);

    private ModuleHandler springModuleHandler;
    private ArtifactContainerFactory containerFactory;
    private AdaptableModuleFactory adaptableFactory;
    private List<Container> springBootSupport;
    private ExecutorService executor;
    private LibIndexCache libIndexCache;

    @Reference(target = "(type=" + SPRING_APP_TYPE + ")")
    protected void setSprModuleHandler(ModuleHandler handler) {
        this.springModuleHandler = handler;
    }

    @Reference(target = "(&(category=DIR)(category=JAR)(category=BUNDLE))")
    protected void setArtifactContainerFactory(ArtifactContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    @Reference
    protected void setAdaptableModuleFactory(AdaptableModuleFactory adaptableFactory) {
        this.adaptableFactory = adaptableFactory;
    }

    @Reference
    protected void setSpringBootSupport(SpringBootSupport support, ServiceReference<SpringBootSupport> ref) {
        Bundle supportBundle = ref.getBundle();
        Container bundleContainer = getContainerForBundle(supportBundle);
        List<Container> supportContainers = new ArrayList<>();
        for (String path : support.getJarPaths()) {
            Entry entry = bundleContainer.getEntry(path);
            try {
                Container pathContainer = entry.adapt(Container.class);
                supportContainers.add(pathContainer);
            } catch (UnableToAdaptException e) {
                // auto generate FFDC
            }
        }
        springBootSupport = Collections.unmodifiableList(supportContainers);
    }

    @Reference
    protected void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    ExecutorService getExecutor() {
        return executor;
    }

    @Reference
    protected void setLibIndexCache(LibIndexCache libIndexCache) {
        this.libIndexCache = libIndexCache;
    }

    @Override
    public SpringDeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        //expandApp(applicationInformation);
        SpringDeployedAppInfo deployedApp = null;
        try {
            storeLibs(applicationInformation);
            deployedApp = new SpringDeployedAppInfo(applicationInformation, this);
            applicationInformation.setHandlerInfo(deployedApp);

        } catch (UnableToAdaptException e) {
            // Log error and continue to use the container for the SPR file
            Tr.error(tc, "warning.could.not.expand.application", applicationInformation.getName(), e.getMessage());
        }
        return deployedApp;
    }

    /**
     * @param applicationInformation
     * @throws UnableToAdaptException
     * @throws NoSuchAlgorithmException
     */
    private void storeLibs(ApplicationInformation<DeployedAppInfo> applicationInformation) throws UnableToAdaptException {
        String location = applicationInformation.getLocation();
        if (location.toLowerCase().endsWith(XML_SUFFIX)) {
            // don't do this for loose applications
            return;
        }

        Container container = applicationInformation.getContainer();
        Entry entry = container.getEntry(SPRING_LIB_INDEX_FILE);
        if (entry != null) {
            // pre-built index is available; use it as-is
            return;
        }

        File springAppFile = new File(location);
        // Make sure the spring thin apps directory is available
        WsResource thinAppsDir = getLocationAdmin().resolveResource(SPRING_THIN_APPS_DIR);
        thinAppsDir.create();

        WsResource thinSpringAppResource = getLocationAdmin().resolveResource(SPRING_THIN_APPS_DIR + applicationInformation.getName() + "." + SPRING_APP_TYPE);
        File thinSpringAppFile = thinSpringAppResource.asFile();
        try {
            if (thinSpringAppFile.exists()) {
                // If the Spring app file has been changed, delete the thin app file
                if (thinSpringAppFile.lastModified() != springAppFile.lastModified()) {
                    thinSpringApp(springAppFile, thinSpringAppFile, springAppFile.lastModified());
                }
            } else {
                thinSpringApp(springAppFile, thinSpringAppFile, springAppFile.lastModified());
            }

            // Set up the new container pointing to the thin spring app file
            container = setupContainer(applicationInformation.getPid(), thinSpringAppFile);
            applicationInformation.setContainer(container);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new UnableToAdaptException(e);
        }
    }

    /**
     * @param springAppFile
     * @param thinSpringAppFile
     * @param lastModified
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private void thinSpringApp(File springAppFile, File thinSpringAppFile, long lastModified) throws IOException, NoSuchAlgorithmException {
        File libIndexCacheFile = libIndexCache.getLibIndexRoot();
        SpringBootThinUtil springBootThinUtil = new SpringBootThinUtil(springAppFile, thinSpringAppFile, libIndexCacheFile, true);
        springBootThinUtil.execute();
        thinSpringAppFile.setLastModified(lastModified);
    }

    private Container getContainerForBundle(Bundle bundle) {
        //for a bundle, we can use the bundles own private data storage as the cache..
        File cacheDir = ensureDataFileExists(bundle, "cache");
        File cacheDirAdapt = ensureDataFileExists(bundle, "cacheAdapt");
        File cacheDirOverlay = ensureDataFileExists(bundle, "cacheOverlay");
        // Create an artifact API and adaptable Container implementation for the bundle
        ArtifactContainer artifactContainer = containerFactory.getContainer(cacheDir, bundle);
        Container wabContainer = adaptableFactory.getContainer(cacheDirAdapt, cacheDirOverlay, artifactContainer);
        return wabContainer;
    }

    private File ensureDataFileExists(Bundle bundle, String path) {
        File dataFile = bundle.getDataFile(path);
        if (!FileUtils.ensureDirExists(dataFile)) {
            throw new RuntimeException("Failed to create cache directory: " + dataFile.getAbsolutePath());
        }
        return dataFile;
    }

    ModuleHandler getSpringModuleHandler() {
        return springModuleHandler;
    }

    List<Container> getSpringBootSupport() {
        return springBootSupport;
    }

    /**
     * @return
     */
    public LibIndexCache getLibIndexCache() {
        return libIndexCache;
    }
}
