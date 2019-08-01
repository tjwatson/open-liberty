package com.ibm.ws.app.manager.internal;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;

import com.ibm.ws.app.manager.ApplicationManagerComponentManager;
import com.ibm.ws.app.manager.internal.monitor.AppMonitorConfiguratorComponentManager;
import com.ibm.ws.app.manager.internal.monitor.ApplicationMonitorComponentManager;
import com.ibm.ws.app.manager.internal.monitor.DropinMonitorComponentManager;

public class BundleHelper implements ComponentManagerFactory {

    @Override
    public ComponentManager createComponentManager(String componentName) {
        if (true) {
            // haven't done the bind updates yet for app.manager.  For now don't return any ComponentManager's
            return null;
        }
        if ("com.ibm.ws.app.manager.ApplicationManager".equals(componentName)) {
            return new ApplicationManagerComponentManager();
        } else if ("com.ibm.ws.app.manager.internal.monitor.ApplicationMonitor".equals(componentName)) {
            return new ApplicationMonitorComponentManager();
        } else if ("com.ibm.ws.app.manager.internal.monitor.DropinMonitor".equals(componentName)) {
            return new DropinMonitorComponentManager();
        } else if ("com.ibm.ws.app.manager.internal.monitor.AppMonitorConfigurator".equals(componentName)) {
            new AppMonitorConfiguratorComponentManager();
        } else if ("com.ibm.ws.app.manager.internal.ApplicationConfigurator".equals(componentName)) {
            return new ApplicationConfiguratorComponentManager();
        }
        return null;
    }
}