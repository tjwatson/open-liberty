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

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_BOOT_CLASSES_HEADER;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_BOOT_INVOKE_MAIN;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_BOOT_LIB_HEADER;
import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_START_CLASS_HEADER;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.internal.ContextRootUtil;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase;
import com.ibm.ws.app.manager.module.internal.ExtendedModuleInfoImpl;
import com.ibm.ws.app.manager.module.internal.ModuleClassLoaderFactory;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.app.manager.module.internal.WebModuleInfoImpl;
import com.ibm.ws.app.manager.springboot.container.SpringContainer;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ManifestClassPathUtils;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleClassesInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.extended.ModuleMetaDataExtender;
import com.ibm.ws.container.service.metadata.extended.NestedModuleMetaDataFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.DefaultNotification;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.application.handler.DefaultApplicationMonitoringInformation;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.GatewayConfiguration;

class SpringDeployedAppInfo extends DeployedAppInfoBase implements SpringContainer {
    private static final String CONTEXT_ROOT = "context-root";

    private final ModuleHandler springModuleHandler;
    private final ExecutorService executor;
    private final FutureMonitor futureMonitor;
    private final SpringModuleContainerInfo springContainerModuleInfo;
    private final boolean invokeSpringAppMain;
    private final CountDownLatch waitToDeploy = new CountDownLatch(1);

    SpringDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation,
                          SpringDeployedAppInfoFactoryImpl factory) throws UnableToAdaptException {
        super(applicationInformation, factory);
        invokeSpringAppMain = Boolean.parseBoolean(factory.getBundleContext().getProperty(SPRING_BOOT_INVOKE_MAIN));
        springModuleHandler = factory.getSpringModuleHandler();
        executor = factory.getExecutor();
        futureMonitor = factory.getFutureMonitor();
        String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(applicationInformation.getLocation());
        String contextRoot = ContextRootUtil.getContextRoot((String) applicationInformation.getConfigProperty(CONTEXT_ROOT));
        //tWAS doesn't use the ibm-web-ext to obtain the context-root when the WAR exists in an EAR.
        //this call is only valid for WAR-only
        if (contextRoot == null) {
            contextRoot = ContextRootUtil.getContextRoot(getContainer());
        }
        this.springContainerModuleInfo = new SpringModuleContainerInfo(factory.getSpringBootSupport(), springModuleHandler, factory.getModuleMetaDataExtenders().get("web"), factory.getNestedModuleMetaDataFactories().get("web"), applicationInformation.getContainer(), null, moduleURI, moduleClassesInfo, contextRoot);
        moduleContainerInfos.add(springContainerModuleInfo);

        // We need to add to the cache so the container doesn't recalculate this for us
        NonPersistentCache npc = getContainer().adapt(NonPersistentCache.class);
        npc.addToCache(WebModuleClassesInfo.class, new WebModuleClassesInfo() {

            @Override
            public List<ContainerInfo> getClassesContainers() {
                return springContainerModuleInfo.getClassesContainerInfo();
            }
        });
    }

    /**
     * Specify the packages to be imported dynamically into all web apps
     */
    private static final List<String> DYNAMIC_IMPORT_PACKAGE_LIST = Collections.unmodifiableList(Arrays.asList("*"));

    @Override
    public ClassLoader createModuleClassLoader(ModuleInfo moduleInfo, List<ContainerInfo> moduleClassesContainers) {
        if (moduleInfo instanceof WebModuleInfo) {
            ApplicationInfo appInfo = moduleInfo.getApplicationInfo();
            String appName = appInfo.getDeploymentName();
            String moduleName = moduleInfo.getURI();
            ClassLoadingService cls = classLoadingService;
            List<Container> containers = new ArrayList<Container>();
            Iterator<ContainerInfo> infos = moduleClassesContainers.iterator();
            // We want the first item to be at the end of the class path for a spr
            if (infos.hasNext()) {
                infos.next();
                while (infos.hasNext()) {
                    containers.add(infos.next().getContainer());
                }
                // Add the first item to the end.
                containers.add(moduleClassesContainers.get(0).getContainer());
            }

            GatewayConfiguration gwCfg = cls.createGatewayConfiguration().setApplicationName(appName).setDynamicImportPackage(DYNAMIC_IMPORT_PACKAGE_LIST);

            ProtectionDomain protectionDomain = getProtectionDomain();

            ClassLoaderConfiguration clCfg = cls.createClassLoaderConfiguration().setId(cls.createIdentity("SpringModule", appName + "#"
                                                                                                                           + moduleName)).setProtectionDomain(protectionDomain).setIncludeAppExtensions(true);

            return createTopLevelClassLoader(containers, gwCfg, clCfg);
        } else {
            return null;
        }
    }

    @Override
    protected ExtendedApplicationInfo createApplicationInfo() {
        ExtendedApplicationInfo appInfo = appInfoFactory.createApplicationInfo(getName(),
                                                                               springContainerModuleInfo.moduleName,
                                                                               getContainer(),
                                                                               this,
                                                                               getConfigHelper());
        springContainerModuleInfo.moduleName = appInfo.getName();
        // ??? Contrary to the EE specs, we use the deployment name, not the EE
        // application name, as the default context root for compatibility.
        springContainerModuleInfo.defaultContextRoot = getName();
        return appInfo;
    }

    @Override
    public List<ModuleClassesContainerInfo> getModuleClassesContainerInfo() {
        return Collections.singletonList((ModuleClassesContainerInfo) springContainerModuleInfo);
    }

    /**
     * @return
     */
    public ApplicationMonitoringInformation createApplicationMonitoringInformation(Container originalContainer) {

        // Only monitor the /BOOT-INF directory in the container
        Notification bootInfNotification = new DefaultNotification(applicationInformation.getContainer(), "/BOOT-INF");
        Notification metaInfNotification = new DefaultNotification(applicationInformation.getContainer(), "/META-INF");
        Collection<Notification> notifications = new HashSet<Notification>();
        notifications.add(bootInfNotification);
        notifications.add(metaInfNotification);

        if (originalContainer != applicationInformation.getContainer()) {
            Notification oldBoot = new DefaultNotification(originalContainer, "/BOOT-INF");
            Notification oldMeta = new DefaultNotification(originalContainer, "/META-INF");
            notifications.add(oldBoot);
            notifications.add(oldMeta);
        }
        return new DefaultApplicationMonitoringInformation(notifications, false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase#preDeployApp(java.util.concurrent.Future)
     */
    @Override
    public boolean preDeployApp(Future<Boolean> result) {
        final AtomicBoolean success = new AtomicBoolean(true);
        if (super.preDeployApp(result)) {
            registerSpringContainerService();

            Future<Boolean> mainInvokeResult = futureMonitor.createFuture(Boolean.class);

            futureMonitor.onCompletion(mainInvokeResult, new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    waitToDeploy.countDown();
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    success.set(false);
                    waitToDeploy.countDown();
                    futureMonitor.setResult(result, t);
                }
            });
            invokeSpringMain(mainInvokeResult);

            try {
                waitToDeploy.await();
            } catch (InterruptedException e) {
                futureMonitor.setResult(result, e);
                return false;
            }
        }
        return success.get();
    }

    /**
     *
     */
    private void registerSpringContainerService() {
        ClassLoader cl = springContainerModuleInfo.getClassLoader();
        while (cl != null && !(cl instanceof BundleReference)) {
            cl = cl.getParent();
        }
        if (cl == null) {
            throw new IllegalStateException("Did not find a BundleReference class loader.");
        }
        Bundle b = ((BundleReference) cl).getBundle();
        BundleContext context = b.getBundleContext();
        context.registerService(SpringContainer.class, this, null);
    }

    private void invokeSpringMain(Future<Boolean> mainInvokeResult) {
        if (!invokeSpringAppMain) {
            futureMonitor.setResult(mainInvokeResult, true);
            return;
        }
        final Method main;
        try {
            Class<?> springAppClass = springContainerModuleInfo.getClassLoader().loadClass(springContainerModuleInfo.springStartClass);
            main = springAppClass.getMethod("main", String[].class);
            main.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            futureMonitor.setResult(mainInvokeResult, e);
            return;
        }

        executor.execute(() -> {
            try {
                // TODO figure out how to pass arguments
                main.invoke(null, new Object[] { new String[0] });
                futureMonitor.setResult(mainInvokeResult, true);
            } catch (InvocationTargetException e) {
                futureMonitor.setResult(mainInvokeResult, e.getTargetException());
            } catch (IllegalAccessException | IllegalArgumentException e) {
                // Auto FFDC here this should not happen
                futureMonitor.setResult(mainInvokeResult, e);
            }
        });
    }

    private static final class SpringModuleContainerInfo extends ModuleContainerInfoBase {
        /**
         * The explicitly specified context root from application.xml, web
         * extension, or server configuration.
         */
        public final String contextRoot;
        final String springStartClass;
        final String springBootClasses;
        final String springBootLib;
        public String defaultContextRoot;

        @FFDCIgnore(IOException.class)
        public SpringModuleContainerInfo(List<Container> springBootSupport, ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                         List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                         Container moduleContainer, Entry altDDEntry,
                                         String moduleURI, ModuleClassesInfoProvider moduleClassesInfo,
                                         String contextRoot) throws UnableToAdaptException {
            super(moduleHandler, moduleMetaDataExtenders, nestedModuleMetaDataFactories, moduleContainer, altDDEntry, moduleURI, ContainerInfo.Type.WEB_MODULE, moduleClassesInfo, WebApp.class);

            Entry manifestEntry = moduleContainer.getEntry("META-INF/MANIFEST.MF");
            try {
                Manifest mf = new Manifest(manifestEntry.adapt(InputStream.class));
                springStartClass = mf.getMainAttributes().getValue(SPRING_START_CLASS_HEADER);
                springBootClasses = removeTrailingSlash(mf.getMainAttributes().getValue(SPRING_BOOT_CLASSES_HEADER));
                springBootLib = removeTrailingSlash(mf.getMainAttributes().getValue(SPRING_BOOT_LIB_HEADER));
            } catch (IOException e) {
                throw new UnableToAdaptException(e);
            }
            getSpringAppClassesInfo(moduleContainer, springBootSupport);
            this.contextRoot = contextRoot;
            this.defaultContextRoot = moduleName;
        }

        private static String removeTrailingSlash(String path) {
            if (path != null && path.length() > 1 && path.endsWith("/")) {
                return path.substring(0, path.length() - 1);
            }
            return path;
        }

        @Override
        public void setModuleName(String newModuleName) {
            super.setModuleName(newModuleName);
            this.defaultContextRoot = newModuleName;
        }

        @Override
        public ExtendedModuleInfoImpl createModuleInfoImpl(ApplicationInfo appInfo,
                                                           ModuleClassLoaderFactory moduleClassLoaderFactory) throws MetaDataException {
            try {
                String contextRoot = this.contextRoot;
                /** Field to verify if Default Context Root is being used */
                boolean isDefaultContextRootUsed = false;
                if (contextRoot == null) {
                    /**
                     * If the module name is equal to the default context root,
                     * it means that the default context root is being used.
                     */
                    if (moduleName.equals(defaultContextRoot)) {
                        isDefaultContextRootUsed = true;
                    }
                    contextRoot = ContextRootUtil.getContextRoot(defaultContextRoot);
                }
                WebModuleInfoImpl webModuleInfo = new WebModuleInfoImpl(appInfo, moduleName, name, contextRoot, container, altDDEntry, classesContainerInfo, moduleClassLoaderFactory);
                /** Set the Default Context Root information to the web module info */
                webModuleInfo.setDefaultContextRootUsed(isDefaultContextRootUsed);
                return webModuleInfo;
            } catch (UnableToAdaptException e) {
                FFDCFilter.processException(e, getClass().getName(), "createModuleInfo", this);
                return null;
            }
        }

        private void getSpringAppClassesInfo(Container moduleContainer, List<Container> springBootSupport) throws UnableToAdaptException {
            ArrayList<String> resolved = new ArrayList<String>();

            Entry classesEntry = moduleContainer.getEntry(springBootClasses);
            if (classesEntry != null) {
                final Container classesContainer = classesEntry.adapt(Container.class);
                if (classesContainer != null) {
                    ContainerInfo containerInfo = new ContainerInfoImpl(Type.WEB_INF_CLASSES, springBootClasses, classesContainer);
                    this.classesContainerInfo.add(containerInfo);
                }
            }

            Entry libEntry = moduleContainer.getEntry(springBootLib);
            if (libEntry != null) {
                Container libContainer = libEntry.adapt(Container.class);
                if (libContainer != null) {
                    for (Entry entry : libContainer) {
                        if (entry.getName().toLowerCase().endsWith(".jar") && !entry.getName().contains("tomcat-")) {
                            String jarEntryName = entry.getName();
                            Container jarContainer = entry.adapt(Container.class);
                            if (jarContainer != null) {
                                ContainerInfo containerInfo = new ContainerInfoImpl(Type.WEB_INF_LIB, springBootLib + '/' + jarEntryName, jarContainer);
                                this.classesContainerInfo.add(containerInfo);
                                ManifestClassPathUtils.addCompleteJarEntryUrls(this.classesContainerInfo, entry, resolved);
                            }
                        }
                    }
                }
                for (Container supportContainer : springBootSupport) {
                    Entry supportEntry = supportContainer.adapt(Entry.class);
                    ContainerInfo containerInfo = new ContainerInfoImpl(Type.WEB_INF_LIB, springBootLib + '/' + supportEntry.getName(), supportContainer);
                    this.classesContainerInfo.add(containerInfo);
                }
            }
        }
    }

    static class ContainerInfoImpl implements ContainerInfo {
        private final Type type;
        private final String name;
        private final Container container;

        public ContainerInfoImpl(Type type, String name, Container container) {
            super();
            this.type = type;
            this.name = name;
            this.container = container;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Container getContainer() {
            return container;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.springboot.container.SpringContainer#configure(java.util.Map)
     */
    @Override
    public void configure(Map<String, String> config) {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.springboot.container.SpringContainer#deploy()
     */
    @Override
    public void deploy() {
        waitToDeploy.countDown();
    }
}
