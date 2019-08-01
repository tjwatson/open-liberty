package com.ibm.ws.logging.internal.osgi;

import java.util.concurrent.ScheduledExecutorService;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;
import org.apache.felix.scr.component.manager.Parameters;
import org.apache.felix.scr.component.manager.ReturnValue;
import org.osgi.service.component.ComponentContext;

public class FFDCJanitorComponentManager implements ComponentManager, ComponentManagerFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((FFDCJanitor) instance).activate();
        return ReturnValue.VOID;
    }

    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((FFDCJanitor) instance).deactivate(reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setScheduler".equals(name)) {
            Object[] params = parameters.getParameters(ScheduledExecutorService.class);
            ((FFDCJanitor) instance).setScheduler((ScheduledExecutorService) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetScheduler".equals(name)) {
            Object[] params = parameters.getParameters(ScheduledExecutorService.class);
            ((FFDCJanitor) instance).unsetScheduler((ScheduledExecutorService) params[0]);
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
        if (FFDCJanitor.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
