/*******************************************************************************n * Copyright (c) 2026 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License 2.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0n *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package test.liberty.runtime;

import static io.openliberty.runtime.LibertyRuntimeConstants.RUNTIME_SHUTDOWN_HOOKS_ADD_JNDI_NAME;
import static io.openliberty.runtime.LibertyRuntimeConstants.RUNTIME_SHUTDOWN_HOOKS_REMOVE_JNDI_NAME;
import static io.openliberty.runtime.LibertyRuntimeConstants.TYPE_PROPERTY;
import static io.openliberty.runtime.LibertyRuntimeConstants.Type.SHUTDOWN_HOOK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import io.openliberty.runtime.LibertyRuntime;

/**
 *
 */
@Component(immediate = true, configurationPid = "test.server.libertyruntime", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TestLibertyRuntime {

    private final Collection<Runnable> hooks = new ArrayList<>();
    private final boolean throwException;
    private final boolean takeForever;
    private final boolean addRemoveMethod;
    private final boolean addMethod;
    private final boolean addGarbageCollection;

    static abstract class AbstractTestHook implements Runnable {
        private static final AtomicInteger nextId = new AtomicInteger();
        private final int id;

        private volatile ServiceRegistration<Runnable> reg;

        public AbstractTestHook() {
            this.id = nextId.getAndIncrement();
        }

        @Override
        public String toString() {
            return getClass().getName() + ": id=" + id;
        }

        public void register(BundleContext context) {
            reg = context.registerService(Runnable.class, this, FrameworkUtil.asDictionary(Collections.singletonMap(TYPE_PROPERTY, SHUTDOWN_HOOK)));
        }

        public void unregister() {
            ServiceRegistration<Runnable> current = reg;
            if (current != null) {
                current.unregister();
            }
        }
    }

    static class ThrowExceptionHook extends AbstractTestHook {
        @Override
        public void run() {
            System.out.println("MUAHAHA.. I will now throw an exception: " + this);
            throw new RuntimeException("WOOPS! I was told to do this, honest: " + this);
        }
    }

    static class TakeForeverHook extends AbstractTestHook {
        @Override
        public void run() {
            System.out.println("MUAHAHA.. I will now take forever to quiesce (literally)! " + this);

            //Rather than deal with slow hardware or possible timing windows, just wait forever
            //The server will still stop. But this gives it ample time to get to the timeout
            //without having to worry about failures that aren't really failures
            //This now relies on the quiesce thread pool to hit the timeout and shutdown
            while (true) {
            }
        }
    }

    static class TestShutdownHook extends AbstractTestHook {
        @Override
        public void run() {
            System.out.println("Running: " + this);
        }

    }

    @Activate
    public TestLibertyRuntime(Map<String, Object> config, BundleContext context,
                              @Reference LibertyRuntime runtime,
                              @Reference(target = "(osgi.jndi.service.name=" + RUNTIME_SHUTDOWN_HOOKS_ADD_JNDI_NAME + ")") Consumer<Runnable> addShutdownHook,
                              @Reference(target = "(osgi.jndi.service.name=" + RUNTIME_SHUTDOWN_HOOKS_REMOVE_JNDI_NAME + ")") Consumer<Runnable> removeShutdownHook) {
        throwException = (Boolean) config.get("throwException");
        takeForever = (Boolean) config.get("takeForever");
        addRemoveMethod = (Boolean) config.get("addRemoveMethod");
        addMethod = (Boolean) config.get("addMethod");
        addGarbageCollection = (Boolean) config.get("addGarbageCollection");

        Runnable hook1 = null;
        Runnable hook2 = null;
        AbstractTestHook hook3 = null;
        if (throwException) {
            hook1 = new ThrowExceptionHook();
            hook2 = new ThrowExceptionHook();
            hook3 = new ThrowExceptionHook();
        } else if (takeForever) {
            hook1 = new TakeForeverHook();
            hook2 = new TakeForeverHook();
            hook3 = new TakeForeverHook();
        } else if (addRemoveMethod || addMethod || addGarbageCollection) {
            hook1 = new TestShutdownHook();
            hook2 = new TestShutdownHook();
            hook3 = new TestShutdownHook();
        }

        if (hook1 == null) {
            System.out.println("No user extension hooks configured: " + config);
            return;
        }

        if (!addGarbageCollection) {
            // prevent Garbage Collection
            hooks.add(hook1);
            hooks.add(hook2);
            hooks.add(hook3);
        }

        runtime.addShutdownHook(hook1);
        addShutdownHook.accept(hook2);
        hook3.register(context);
        if (addRemoveMethod) {
            runtime.removeShutdownHook(hook1);
            removeShutdownHook.accept(hook2);
            hook3.unregister();
        }
        if (addGarbageCollection) {
            hook1 = null;
            hook2 = null;
            hook3 = null;
            // run GC to collect the hook
            System.gc();
            System.gc();
            System.gc();
        }
    }
}
