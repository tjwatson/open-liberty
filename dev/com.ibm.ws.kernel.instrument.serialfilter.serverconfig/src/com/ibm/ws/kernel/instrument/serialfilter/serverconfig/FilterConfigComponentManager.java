package com.ibm.ws.kernel.instrument.serialfilter.serverconfig;

import java.util.Map;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;
import org.apache.felix.scr.component.manager.Parameters;
import org.apache.felix.scr.component.manager.ReturnValue;
import org.osgi.service.component.ComponentContext;

public class FilterConfigComponentManager implements ComponentManager, ComponentManagerFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((FilterConfigFactory) instance).activate(componentContext, (Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((FilterConfigFactory) instance).deactivate(componentContext, reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        ((FilterConfigFactory) instance).modified((Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
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
        if (FilterConfigFactory.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
