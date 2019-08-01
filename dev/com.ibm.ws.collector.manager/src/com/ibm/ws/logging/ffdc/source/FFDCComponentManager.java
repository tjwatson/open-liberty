/*******************************************************************************n * Copyright (c) 2019 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.logging.ffdc.source;

import java.util.Map;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.Parameters;
import org.apache.felix.scr.component.manager.ReturnValue;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.collector.manager.BufferManager;

/**
 *
 */
public class FFDCComponentManager implements ComponentManager {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((FFDCSource) instance).activate((Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((FFDCSource) instance).deactivate(reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setBufferManager".equals(name)) {
            Object[] params = parameters.getParameters(BufferManager.class);
            ((FFDCSource) instance).setBufferManager((BufferManager) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetBufferManager".equals(name)) {
            Object[] params = parameters.getParameters(BufferManager.class);
            ((FFDCSource) instance).unsetBufferManager((BufferManager) params[0]);
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

}
