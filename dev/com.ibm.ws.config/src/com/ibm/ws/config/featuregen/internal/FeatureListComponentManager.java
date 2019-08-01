/*******************************************************************************n * Copyright (c) 2019 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.config.featuregen.internal;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.Parameters;
import org.apache.felix.scr.component.manager.ReturnValue;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 *
 */
public class FeatureListComponentManager implements ComponentManager {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((FeatureListMBeanImpl) instance).activate(componentContext);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((FeatureListMBeanImpl) instance).deactivate(componentContext);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setLocationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((FeatureListMBeanImpl) instance).setLocationAdmin((ServiceReference<WsLocationAdmin>) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetLocationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((FeatureListMBeanImpl) instance).unsetLocationAdmin((ServiceReference<WsLocationAdmin>) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue updated(Object instance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }

}
