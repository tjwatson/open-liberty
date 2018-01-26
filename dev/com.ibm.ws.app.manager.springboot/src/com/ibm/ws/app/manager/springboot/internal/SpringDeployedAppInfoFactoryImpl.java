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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import com.ibm.ws.app.manager.springboot.internal.SpringDeployedAppInfo.SpringBootManifest;
import com.ibm.ws.app.manager.springboot.support.SpringBootSupport;
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
    public SpringDeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation) throws UnableToAdaptException {
        //expandApp(applicationInformation);

        storeLibs(applicationInformation);

        SpringDeployedAppInfo deployedApp = new SpringDeployedAppInfo(applicationInformation, this);
        applicationInformation.setHandlerInfo(deployedApp);
        return deployedApp;
    }

    /**
     * @param applicationInformation
     */
    private void storeLibs(ApplicationInformation<DeployedAppInfo> applicationInformation) {
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

        WsResource thinSpringAppFile = getLocationAdmin().resolveResource(SPRING_THIN_APPS_DIR + applicationInformation.getName() + "." + SPRING_APP_TYPE);
        try {
            if (thinSpringAppFile.exists()) {
                // If the Spring app file has been changed, delete the thin app file
                if (thinSpringAppFile.getLastModified() != springAppFile.lastModified()) {
                    thinSpringApp(applicationInformation, container, thinSpringAppFile, springAppFile.lastModified());
                }
            } else {
                thinSpringApp(applicationInformation, container, thinSpringAppFile, springAppFile.lastModified());
            }

            // Set up the new container pointing to the thin spring app file
            container = setupContainer(applicationInformation.getPid(), thinSpringAppFile.asFile());
            applicationInformation.setContainer(container);
        } catch (UnableToAdaptException | IOException e) {
            // Log error and continue to use the container for the SPR file
            Tr.error(tc, "warning.could.not.expand.application", applicationInformation.getName(), e.getMessage());
        }
    }

    /**
     * @param container
     * @param thinSpringAppFile
     * @throws UnableToAdaptException
     */
    private void thinSpringApp(ApplicationInformation<DeployedAppInfo> applicationInformation, Container container,
                               WsResource thinSpringAppFile, long timestamp) throws UnableToAdaptException, IOException {
        SpringBootManifest sprMF = new SpringBootManifest(container);
        thinSpringAppFile.delete();
        File thinFile = thinSpringAppFile.asFile();
        try (ZipOutputStream thinJar = new ZipOutputStream(new FileOutputStream(thinFile))) {
            for (Entry entry : container) {
                storeEntry(thinJar, sprMF, entry);
            }
        }

        thinFile.setLastModified(timestamp);

    }

    /**
     * @param thinJar
     * @param sprMF
     * @param entry
     * @throws IOException
     * @throws UnableToAdaptException
     */
    private void storeEntry(ZipOutputStream thinJar, SpringBootManifest sprMF, Entry entry) throws UnableToAdaptException, IOException {
        String path = entry.getPath();
        if (path.length() > 1 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        if (path.equals(sprMF.springBootLib)) {
            storeLibDirEntry(thinJar, entry);
        } else {
            try (InputStream in = entry.adapt(InputStream.class)) {
                if (in == null) {
                    // must be a directory
                    ZipEntry dirEntry = new ZipEntry(path + '/');
                    thinJar.putNextEntry(dirEntry);
                    thinJar.closeEntry();
                    for (Entry nested : entry.adapt(Container.class)) {
                        storeEntry(thinJar, sprMF, nested);
                    }
                } else {
                    try {
                        thinJar.putNextEntry(new ZipEntry(path));
                        byte[] buffer = new byte[1024];
                        int read = -1;
                        while ((read = in.read(buffer)) != -1) {
                            thinJar.write(buffer, 0, read);
                        }
                    } finally {
                        thinJar.closeEntry();
                    }
                }
            }
        }
    }

    /**
     * @param thinJar
     * @param entry
     * @throws UnableToAdaptException
     * @throws IOException
     */
    private void storeLibDirEntry(ZipOutputStream thinJar, Entry libEntry) throws UnableToAdaptException, IOException {
        // create the lib folder entry first
        ZipEntry dirEntry = new ZipEntry(libEntry.getPath() + '/');
        thinJar.putNextEntry(dirEntry);
        thinJar.closeEntry();

        // write out each library to the cache
        List<String> libEntries = new ArrayList<>();
        Container libContainer = libEntry.adapt(Container.class);
        if (libContainer != null) {
            for (Entry entry : libContainer) {
                String hash = libIndexCache.storeLibrary(entry);
                String libLine = libEntry.getPath() + '/' + entry.getName() + '=' + hash;
                libEntries.add(libLine);
                Tr.debug(tc, "Stored library {1}", libLine);
            }
        }

        // save the lib index file in the thin jar
        thinJar.putNextEntry(new ZipEntry(SPRING_LIB_INDEX_FILE));
        try {
            for (String libLine : libEntries) {
                thinJar.write(libLine.getBytes(StandardCharsets.UTF_8));
                thinJar.write('\n');
            }
        } finally {
            thinJar.closeEntry();
        }
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
