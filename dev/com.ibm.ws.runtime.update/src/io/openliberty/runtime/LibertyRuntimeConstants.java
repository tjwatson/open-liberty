/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.runtime;

/**
 *
 */
public interface LibertyRuntimeConstants {
    static final String RUNTIME_JNDI_NAME = "ol/runtime/LibertyRuntime";
    static final String RUNTIME_SHUTDOWN_HOOKS_ADD_JNDI_NAME = "ol/runtime/shutdownHooks/add";
    static final String RUNTIME_SHUTDOWN_HOOKS_REMOVE_JNDI_NAME = "ol/runtime/shutdownHooks/remove";
    static final String TYPE_PROPERTY = "type";

    public enum Type {
        SHUTDOWN_HOOK
    }
}
