package com.ibm.ws.collector.manager.internal;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;

import com.ibm.ws.logging.ffdc.source.FFDCComponentManager;
import com.ibm.ws.logging.ffdc.source.FFDCSource;

public class BundleHelper implements ComponentManagerFactory {

    @Override
    public ComponentManager createComponentManager(String componentName) {
        if (CollectorManagerImpl.class.getName().equals(componentName)) {
            return new CollectorComponentManager();
        }
        if (FFDCSource.class.getName().equals(componentName)) {
            return new FFDCComponentManager();
        }
        return null;
    }
}