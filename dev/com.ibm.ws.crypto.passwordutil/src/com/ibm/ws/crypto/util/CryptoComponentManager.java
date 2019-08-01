package com.ibm.ws.crypto.util;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;
import org.apache.felix.scr.component.manager.Parameters;
import org.apache.felix.scr.component.manager.ReturnValue;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.security.crypto.CustomPasswordEncryption;

public class CryptoComponentManager implements ComponentManager, ComponentManagerFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        if (instance instanceof PasswordCipherUtil) {
            ((PasswordCipherUtil) instance).activate(componentContext);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        if (instance instanceof PasswordCipherUtil) {
            ((PasswordCipherUtil) instance).deactivate(componentContext);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setCustomPasswordEncryption".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((PasswordCipherUtil) instance).setCustomPasswordEncryption((ServiceReference<CustomPasswordEncryption>) params[0]);
        } else if ("setVariableRegistry".equals(name)) {
            Object[] params = parameters.getParameters(VariableRegistry.class);
            ((VariableResolver) instance).setVariableRegistry((VariableRegistry) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetCustomPasswordEncryption".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((PasswordCipherUtil) instance).unsetCustomPasswordEncryption((ServiceReference<CustomPasswordEncryption>) params[0]);
        } else if ("unsetVariableRegistry".equals(name)) {
            Object[] params = parameters.getParameters(VariableRegistry.class);
            ((VariableResolver) instance).unsetVariableRegistry((VariableRegistry) params[0]);
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
        if (PasswordCipherUtil.class.getName().equals(componentName) || VariableResolver.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
