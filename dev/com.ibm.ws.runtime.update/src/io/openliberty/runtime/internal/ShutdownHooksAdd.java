/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.runtime.internal;

import java.util.function.Consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openliberty.runtime.LibertyRuntime;
import io.openliberty.runtime.LibertyRuntimeConstants;

/**
 *
 */
@Component(property = { "service.vendor=IBM", "osgi.jndi.service.name=" + LibertyRuntimeConstants.RUNTIME_SHUTDOWN_HOOKS_ADD_JNDI_NAME,
                        "osgi.jndi.service.class=java.util.function.Consumer" })
public class ShutdownHooksAdd implements Consumer<Runnable> {

    private final LibertyRuntime runtime;

    @Activate
    public ShutdownHooksAdd(@Reference LibertyRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void accept(Runnable hook) {
        runtime.addShutdownHook(hook);
    }

}
