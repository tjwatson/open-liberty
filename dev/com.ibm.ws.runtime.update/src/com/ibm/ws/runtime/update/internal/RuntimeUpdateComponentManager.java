package com.ibm.ws.runtime.update.internal;

import java.util.concurrent.ExecutorService;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;
import org.apache.felix.scr.component.manager.Parameters;
import org.apache.felix.scr.component.manager.ReturnValue;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

public class RuntimeUpdateComponentManager implements ComponentManager, ComponentManagerFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        if (instance instanceof RuntimeUpdateManagerImpl) {
            ((RuntimeUpdateManagerImpl) instance).activate(componentContext.getBundleContext());
        } else if (instance instanceof PauseableComponentQuiesceListener) {
            ((PauseableComponentQuiesceListener) instance).activate(componentContext.getBundleContext());
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setExecutorService".equals(name)) {
            Object[] params = parameters.getParameters(ExecutorService.class);
            ((RuntimeUpdateManagerImpl) instance).setExecutorService((ExecutorService) params[0]);
        } else if ("setFutureMonitor".equals(name)) {
            Object[] params = parameters.getParameters(FutureMonitor.class);
            ((RuntimeUpdateManagerImpl) instance).setFutureMonitor((FutureMonitor) params[0]);
        } else if ("setLocationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(WsLocationAdmin.class);
            ((RuntimeUpdateManagerImpl) instance).setLocationAdmin((WsLocationAdmin) params[0]);
        } else if ("setRuntimeUpdateListener".equals(name)) {
            Object[] params = parameters.getParameters(RuntimeUpdateListener.class);
            ((RuntimeUpdateManagerImpl) instance).setRuntimeUpdateListener((RuntimeUpdateListener) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetFutureMonitor".equals(name)) {
            Object[] params = parameters.getParameters(FutureMonitor.class);
            ((RuntimeUpdateManagerImpl) instance).unsetFutureMonitor((FutureMonitor) params[0]);
        } else if ("unsetLocationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(WsLocationAdmin.class);
            ((RuntimeUpdateManagerImpl) instance).unsetLocationAdmin((WsLocationAdmin) params[0]);
        } else if ("unsetRuntimeUpdateListener".equals(name)) {
            Object[] params = parameters.getParameters(RuntimeUpdateListener.class);
            ((RuntimeUpdateManagerImpl) instance).unsetRuntimeUpdateListener((RuntimeUpdateListener) params[0]);
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
        if (PauseableComponentQuiesceListener.class.getName().equals(componentName) ||
            RuntimeUpdateManagerImpl.class.getName().equals(componentName) ||
            RuntimeUpdateNotificationMBeanImpl.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
