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

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.internal.ContextRootUtil;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase;
import com.ibm.ws.app.manager.module.internal.ExtendedModuleInfoImpl;
import com.ibm.ws.app.manager.module.internal.ModuleClassLoaderFactory;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.app.manager.module.internal.WebModuleInfoImpl;
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
import com.ibm.ws.javaee.dd.web.WebApp;
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

class SpringDeployedAppInfo extends DeployedAppInfoBase {

    private static final String CONTEXT_ROOT = "context-root";

    private final ModuleHandler springModuleHandler;
    private final SpringModuleContainerInfo springContainerModuleInfo;

    SpringDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation,
                          SpringDeployedAppInfoFactoryImpl factory) throws UnableToAdaptException {
        super(applicationInformation, factory);
        this.springModuleHandler = factory.springModuleHandler;

        String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(applicationInformation.getLocation());
        String contextRoot = ContextRootUtil.getContextRoot((String) applicationInformation.getConfigProperty(CONTEXT_ROOT));
        //tWAS doesn't use the ibm-web-ext to obtain the context-root when the WAR exists in an EAR.
        //this call is only valid for WAR-only
        if (contextRoot == null) {
            contextRoot = ContextRootUtil.getContextRoot(getContainer());
        }
        this.springContainerModuleInfo = new SpringModuleContainerInfo(springModuleHandler, factory.getModuleMetaDataExtenders().get("web"), factory.getNestedModuleMetaDataFactories().get("web"), applicationInformation.getContainer(), null, moduleURI, moduleClassesInfo, contextRoot);
        moduleContainerInfos.add(springContainerModuleInfo);

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
            String j2eeAppName = appInfo.getDeploymentName();
            String j2eeModuleName = moduleInfo.getURI();
            ClassLoadingService cls = classLoadingService;
            List<Container> containers = new ArrayList<Container>();
            Iterator<ContainerInfo> infos = moduleClassesContainers.iterator();
            // We want the first item to be at the end of the class path for a war
            if (infos.hasNext()) {
                infos.next();
                while (infos.hasNext()) {
                    containers.add(infos.next().getContainer());
                }
                // Add the first item to the end.
                containers.add(moduleClassesContainers.get(0).getContainer());
            }

            GatewayConfiguration gwCfg = cls.createGatewayConfiguration()
                            // TODO call .setApplicationVersion() with some appropriate value
                            .setApplicationName(j2eeAppName).setDynamicImportPackage(DYNAMIC_IMPORT_PACKAGE_LIST);

            ProtectionDomain protectionDomain = getProtectionDomain();

            ClassLoaderConfiguration clCfg = cls.createClassLoaderConfiguration().setId(cls.createIdentity("WebModule", j2eeAppName + "#"
                                                                                                                        + j2eeModuleName)).setProtectionDomain(protectionDomain).setIncludeAppExtensions(true);

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

    private static final class SpringModuleContainerInfo extends ModuleContainerInfoBase {
        /**
         * The explicitly specified context root from application.xml, web
         * extension, or server configuration.
         */
        public final String contextRoot;
        public String defaultContextRoot;

        public SpringModuleContainerInfo(ModuleHandler moduleHandler, List<ModuleMetaDataExtender> moduleMetaDataExtenders,
                                         List<NestedModuleMetaDataFactory> nestedModuleMetaDataFactories,
                                         Container moduleContainer, Entry altDDEntry,
                                         String moduleURI, ModuleClassesInfoProvider moduleClassesInfo,
                                         String contextRoot) throws UnableToAdaptException {
            super(moduleHandler, moduleMetaDataExtenders, nestedModuleMetaDataFactories, moduleContainer, altDDEntry, moduleURI, ContainerInfo.Type.WEB_MODULE, moduleClassesInfo, WebApp.class);
            getWebModuleClassesInfo(moduleContainer);
            this.contextRoot = contextRoot;
            this.defaultContextRoot = moduleName;
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

        private void getWebModuleClassesInfo(Container moduleContainer) throws UnableToAdaptException {
            ArrayList<String> resolved = new ArrayList<String>();

            Entry classesEntry = moduleContainer.getEntry("BOOT-INF/classes");
            if (classesEntry != null) {
                final Container classesContainer = classesEntry.adapt(Container.class);
                if (classesContainer != null) {
                    ContainerInfo containerInfo = new ContainerInfo() {
                        @Override
                        public Type getType() {
                            return Type.WEB_INF_CLASSES;
                        }

                        @Override
                        public String getName() {
                            return "BOOT-INF/classes";
                        }

                        @Override
                        public Container getContainer() {
                            return classesContainer;
                        }
                    };
                    this.classesContainerInfo.add(containerInfo);
                }
            }

            Entry libEntry = moduleContainer.getEntry("BOOT-INF/lib");
            if (libEntry != null) {
                Container libContainer = libEntry.adapt(Container.class);
                if (libContainer != null) {
                    for (Entry entry : libContainer) {
                        if (entry.getName().toLowerCase().endsWith(".jar") && !entry.getName().contains("tomcat-")) {
                            final String jarEntryName = entry.getName();
                            final Container jarContainer = entry.adapt(Container.class);
                            if (jarContainer != null) {
                                ContainerInfo containerInfo = new ContainerInfo() {
                                    @Override
                                    public Type getType() {
                                        return Type.WEB_INF_LIB;
                                    }

                                    @Override
                                    public String getName() {
                                        return "BOOT-INF/lib/" + jarEntryName;
                                    }

                                    @Override
                                    public Container getContainer() {
                                        return jarContainer;
                                    }
                                };
                                this.classesContainerInfo.add(containerInfo);

                                ManifestClassPathUtils.addCompleteJarEntryUrls(this.classesContainerInfo, entry, resolved);
                            }
                        }
                    }
                }
            }
        }
    }

}
