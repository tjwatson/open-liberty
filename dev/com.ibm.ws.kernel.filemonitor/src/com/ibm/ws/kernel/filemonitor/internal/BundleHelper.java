package com.ibm.ws.kernel.filemonitor.internal;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;

import com.ibm.ws.kernel.filemonitor.internal.scan.ScanningComponentManager;
import com.ibm.ws.kernel.filemonitor.internal.scan.ScanningCoreServiceImpl;

public class BundleHelper implements ComponentManagerFactory {

    @Override
    public ComponentManager createComponentManager(String componentName) {
        if (FileNotificationImpl.class.getName().equals(componentName)) {
            return new FileNotificationComponentManager();
        }
        if (ScanningCoreServiceImpl.class.getName().equals(componentName)) {
            return new ScanningComponentManager();
        }
        return null;
    }
}