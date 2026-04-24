/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.runtime.update.fat;

import java.io.FileNotFoundException;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.IncludeElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import shutdownhooks.ShutdownHooksServlet;

/**
 * This class is a little odd:
 * We're testing the behavior of server stop. One server will be used for all test methods.
 * The server will be started and stopped within each test method, BUT..
 * when the server is stopped within the tests, the logs will not be collected.
 *
 * The server logs will be collected at the end, in the tearDown.
 */
@RunWith(FATRunner.class)
public class LibertyRuntimeTest extends FATServletClient {
    private static final String QUIESCE_LISTENER_HUNG_WARNING = "CWWKE1106W";
    private static final String QUIESCE_FAILURE_WARNING = "CWWKE1102W";

    private static final Class<?> c = LibertyRuntimeTest.class;

    @Server("com.ibm.ws.liberty.runtime.fat")
    @TestServlet(servlet = ShutdownHooksServlet.class, contextRoot = "shutdownhooks")
    public static LibertyServer server;

    @Rule
    public final TestName method = new TestName();

    @BeforeClass
    public static void setUpClass() throws Exception {
        server.installUserBundle("test.liberty.runtime");
        server.installUserFeature("libertyRuntimeTest-1.0");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        server.uninstallUserBundle("test.liberty.runtime");
        server.uninstallUserFeature("libertyRuntimeTest-1.0");
    }

    @Before
    public void startServer() throws Exception {
        WebArchive dropinsApp = ShrinkHelper.buildDefaultApp("shutdownhooks", "shutdownhooks");
        ShrinkHelper.exportDropinAppToServer(server, dropinsApp);

        ServerConfiguration config = server.getServerConfiguration();
        config.getIncludes().removeIf((i) -> !("../fatTestPorts.xml".equals(i.getLocation())));
        String testConfigPath = "test.configs/" + method.getMethodName() + ".xml";
        try {
            server.getFileFromLibertyServerRoot(testConfigPath);
            IncludeElement include = new IncludeElement();
            include.setLocation(testConfigPath);
            config.getIncludes().add(include);
        } catch (FileNotFoundException e) {
            // No config for this test;
        }

        server.updateServerConfiguration(config);

        server.startServer(method.getMethodName() + ".console.log");
    }

    @After
    public void stopServer() throws Exception {
        try {
            // make sure server is torn down
            if (server.isStarted()) {
                if (method.getMethodName().contains("TakeForever")) {
                    server.stopServer(QUIESCE_FAILURE_WARNING, QUIESCE_LISTENER_HUNG_WARNING);
                } else {
                    server.stopServer();
                }
            }
        } finally {
            Log.info(c, method.getMethodName(), "**** EXIT: " + method.getMethodName());
        }
    }

    @Test
    public void testAddShutdownHook() throws Exception {
    }

    @Test
    public void testTakeForeverHook() throws Exception {
    }

    @Test
    public void testAddRemoveShutdownHook() throws Exception {
    }

    @Test
    public void testAddGarbageCollectionShutdownHook() throws Exception {
    }

    @Test
    @ExpectedFFDC("java.lang.RuntimeException")
    public void testThrowExceptionShutdownHook() throws Exception {
    }

}
