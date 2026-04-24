/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package shutdownhooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import io.openliberty.runtime.LibertyRuntime;
import io.openliberty.runtime.LibertyRuntimeConstants;

/**
 *
 */
@WebServlet(urlPatterns = "/ShutdownHooksServlet", loadOnStartup = 1)
public class ShutdownHooksServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    static abstract class AbstractTestHook implements Runnable {
        private static final AtomicInteger nextId = new AtomicInteger();
        private final int id;

        public AbstractTestHook() {
            this.id = nextId.getAndIncrement();
        }

        @Override
        public String toString() {
            return getClass().getName() + ": id=" + id;
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

    private final Collection<Runnable> hooks = new ArrayList<>();

    @Resource(lookup = LibertyRuntimeConstants.RUNTIME_JNDI_NAME)
    private LibertyRuntime libertyRuntime;

    @Resource(lookup = LibertyRuntimeConstants.RUNTIME_SHUTDOWN_HOOKS_ADD_JNDI_NAME)
    private Consumer<Runnable> addHook;

    @Resource(lookup = LibertyRuntimeConstants.RUNTIME_SHUTDOWN_HOOKS_REMOVE_JNDI_NAME)
    private Consumer<Runnable> removeHook;

    @Override
    public void destroy() {
        System.out.println("Application is stoppping.");
        new RuntimeException("Application is stoppping.").printStackTrace();
    }

    @Test
    public void testApplicationAddShutdownHook() throws Exception {
        Runnable hook1 = new TestShutdownHook();
        Runnable hook2 = new TestShutdownHook();
        hooks.add(hook1);
        hooks.add(hook2);
        libertyRuntime.addShutdownHook(hook1);
        addHook.accept(hook2);
    }

    @Test
    public void testApplicationAddRemoveShutdownHook() throws Exception {
        Runnable hook1 = new TestShutdownHook();
        Runnable hook2 = new TestShutdownHook();
        hooks.add(hook1);
        hooks.add(hook2);
        // Add hooks
        libertyRuntime.addShutdownHook(hook1);
        addHook.accept(hook2);
        // Remove hooks
        libertyRuntime.removeShutdownHook(hook1);
        removeHook.accept(hook2);
    }

    @Test
    public void testApplicationTakeForeverHook() throws Exception {
        Runnable hook1 = new TakeForeverHook();
        Runnable hook2 = new TakeForeverHook();
        hooks.add(hook1);
        hooks.add(hook2);
        libertyRuntime.addShutdownHook(hook1);
        addHook.accept(hook2);
    }

    @Test
    @ExpectedFFDC("java.lang.RuntimeException")
    public void testApplicationThrowExceptionHook() throws Exception {
        Runnable hook1 = new ThrowExceptionHook();
        Runnable hook2 = new ThrowExceptionHook();
        hooks.add(hook1);
        hooks.add(hook2);
        libertyRuntime.addShutdownHook(hook1);
        addHook.accept(hook2);
    }

    @Test
    public void testApplicationGarbageCollectionHook() throws Exception {
        Runnable hook1 = new ThrowExceptionHook();
        Runnable hook2 = new ThrowExceptionHook();
        libertyRuntime.addShutdownHook(hook1);
        addHook.accept(hook2);

        System.gc();
        System.gc();
        System.gc();
    }
}
