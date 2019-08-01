package com.ibm.ws.threading.internal;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;

import com.ibm.ws.threading.PolicyExecutorComponentManager;
import com.ibm.ws.threading.PolicyExecutorProvider;

public class BundleHelper implements ComponentManagerFactory {

    @Override
    public ComponentManager createComponentManager(String componentName) {
        if (PolicyExecutorProvider.class.getName().equals(componentName)) {
            return new PolicyExecutorComponentManager();
        }
        if (DeferrableScheduledExecutorImpl.class.getName().equals(componentName)) {
            return new DeferrableExecutorComponentManager();
        }
        if (FutureMonitorImpl.class.getName().equals(componentName)) {
            return new FutureMonitorComponentManager();
        }
        if (ScheduledExecutorImpl.class.getName().equals(componentName)) {
            return new ScheduledExecutorComponentManager();
        }
        if (ThreadingIntrospector.class.getName().equals(componentName)) {
            return new ThreadingIntrospectorComponentManager();
        }
        if (ExecutorServiceImpl.class.getName().equals(componentName)) {
            return new ExecutorServiceComponentManager();
        }
        return null;
    }
}