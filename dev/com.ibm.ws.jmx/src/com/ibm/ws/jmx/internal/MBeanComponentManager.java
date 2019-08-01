package com.ibm.ws.jmx.internal;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;
import org.apache.felix.scr.component.manager.Parameters;
import org.apache.felix.scr.component.manager.ReturnValue;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;

public class MBeanComponentManager implements ComponentManager, ComponentManagerFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((DelayedMBeanActivatorHelper) instance).activate(componentContext);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((DelayedMBeanActivatorHelper) instance).deactivate(componentContext);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setMBeanServerPipeline".equals(name)) {
            Object[] params = parameters.getParameters(MBeanServerPipeline.class);
            ((DelayedMBeanActivatorHelper) instance).setMBeanServerPipeline((MBeanServerPipeline) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetMBeanServerPipeline".equals(name)) {
            Object[] params = parameters.getParameters(MBeanServerPipeline.class);
            ((DelayedMBeanActivatorHelper) instance).unsetMBeanServerPipeline((MBeanServerPipeline) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue updated(Object componentInstance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }

    @Override
    public ComponentManager createComponentManager(String componentName) {
        if (DelayedMBeanActivatorHelper.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
