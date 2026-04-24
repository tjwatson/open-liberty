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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.runtime.LibertyRuntime;
import io.openliberty.runtime.LibertyRuntimeConstants;

/**
 *
 */
@Component(property = { "service.vendor=IBM", "osgi.jndi.service.name=" + LibertyRuntimeConstants.RUNTIME_JNDI_NAME, "osgi.jndi.service.class=io.openliberty.runtime.Runtime" })
public class RuntimeImpl implements LibertyRuntime {

    static class ShutdownHookReference extends WeakReference<Runnable> implements Runnable {
        private ServiceRegistration<Runnable> reg;

        public ShutdownHookReference(Runnable runnable, ReferenceQueue<Runnable> queue) {
            super(runnable, queue);
        }

        synchronized void register(BundleContext context) {
            reg = context.registerService(Runnable.class, this,
                                          FrameworkUtil.asDictionary(Collections.singletonMap(LibertyRuntimeConstants.TYPE_PROPERTY,
                                                                                              LibertyRuntimeConstants.Type.SHUTDOWN_HOOK.toString())));
        }

        @FFDCIgnore(IllegalStateException.class)
        synchronized void unregister() {
            if (reg != null) {
                try {
                    reg.unregister();
                } catch (IllegalStateException e) {
                    // ignore
                } finally {
                    reg = null;
                }
            }
        }

        @Override
        public void run() {
            Runnable existing = get();
            if (existing != null) {
                existing.run();
            }
        }

        @Override
        public String toString() {
            Runnable existing = get();
            if (existing != null) {
                return existing.toString();
            }
            if (reg != null) {
                return reg.toString();
            }
            return super.toString();
        }
    }

    private final ReferenceQueue<Runnable> hookReferenceQueue = new ReferenceQueue<>();
    private final List<ShutdownHookReference> hooks = Collections.synchronizedList(new ArrayList<>());

    private final BundleContext context;

    @Activate
    public RuntimeImpl(BundleContext context) {
        this.context = context;
    }

    @Deactivate
    void deactivate() {
        hooks.forEach(ShutdownHookReference::unregister);
    }

    @Override
    public void addShutdownHook(Runnable hook) {
        ShutdownHookReference wrapped = new ShutdownHookReference(hook, hookReferenceQueue);
        wrapped.register(context);
        hooks.add(wrapped);
        // clean up GC'ed (null) hooks
        removeShutdownHook(null);

    }

    @Override
    public void removeShutdownHook(Runnable hook) {
        hooks.removeIf((h) -> {
            if (h.get() == null || h.get() == hook) {
                h.unregister();
                return true;
            }
            return false;
        });
    }
}
