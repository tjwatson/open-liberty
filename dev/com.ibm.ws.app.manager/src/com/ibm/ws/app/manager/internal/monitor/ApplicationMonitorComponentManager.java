package com.ibm.ws.app.manager.internal.monitor;

import java.util.Map;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.Parameters;
import org.apache.felix.scr.component.manager.ReturnValue;
import org.osgi.service.component.ComponentContext;

public class ApplicationMonitorComponentManager implements ComponentManager {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((ApplicationMonitor) instance).activate(componentContext, (Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((ApplicationMonitor) instance).deactivate(componentContext, reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        ((ApplicationMonitor) instance).modified((Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object componentInstance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object componentInstance, String name, Parameters parameters) {
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
}