/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.app.manager.wab.configure.fat;

import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ConfigurableWABTests {

    private static final String BUNDLE_TEST_WAB1 = "test.wab1";
    private static final String BUNDLE_TEST_WAB2 = "test.wab2";
    private static final String BUNDLE_TEST_WAB3 = "test.wab3";

    private static final String PRODUCT1 = "product1";
    private static final String PRODUCT2 = "product2";
    private static final String[] PRODUCTS = { PRODUCT1, PRODUCT2 };

    private static final String WAB1 = "/wab1";
    private static final String WAB2 = "/wab2";
    private static final String SERVLET1 = "/servlet1";
    private static final String SERVLET2 = "/servlet2";
    private static final String SERVLET3 = "/servlet3";
    private static final String RESTART = "/restart";
    private static final String SWITCH = "/switch";
    private static final String SWITCH_TARGET = "/switchTarget";
    private static final String CONFLICT = "/conflict";
    private static final String OUTPUT_SERVLET1 = "service: test.wab1.Servlet1";
    private static final String OUTPUT_SERVLET2 = "service: test.wab2.Servlet2";
    private static final String OUTPUT_SERVLET3 = "service: test.wab3.Servlet3";
    private static final String OUTPUT_RESTART = "SUCCESS service: test.wab1.Restart";
    private static final String OUTPUT_SWITCH = "SUCCESS service: test.wab1.Switch";

    private static final String CONFIGS = "configs/";
    private static final String CONFIG_DEFAULT = CONFIGS + "testDefault.xml";
    private static final String CONFIG_MULTIPLE = CONFIGS + "testMultple.xml";
    private static final String CONFIG_CONFLICT = CONFIGS + "testConflict.xml";

    protected static LibertyServer server = null;

    private static final Class<?> c = ConfigurableWABTests.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.app.manager.wab.configure");
        server.setServerConfigurationFile(CONFIG_DEFAULT);

        for (String product : PRODUCTS) {
            Log.info(c, "setUp", "Installing product properties file for: " + product);
            server.installProductExtension(product);

            Log.info(c, "setUp", "Installing product feature: " + product);
            server.installProductFeature(product, product);

            Log.info(c, "setUp", "Installing product bundles: " + product);
            server.installProductBundle(product, product);
            server.installProductBundle(product, BUNDLE_TEST_WAB1);
            server.installProductBundle(product, BUNDLE_TEST_WAB2);
            server.installProductBundle(product, BUNDLE_TEST_WAB3);
        }

        server.startServer();
    }

    @Before
    public void beforeTest() throws Exception {
        Log.info(c, name.getMethodName(), "===== Starting test " + name.getMethodName() + " =====");
    }

    private void setConfiguration(String config) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
    }

    @After
    public void afterTest() throws Exception {
        Log.info(c, name.getMethodName(), "===== Stopping test " + name.getMethodName() + " =====");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (server != null && server.isStarted()) {
                // we expect a conflict error from the conflict test
                server.stopServer("CWWKZ0208E");
            }

        } finally {
            if (server != null) {
                for (String product : PRODUCTS) {
                    server.uninstallProductBundle(product, product + "_1.0.0");
                    server.uninstallProductBundle(product, BUNDLE_TEST_WAB1);
                    server.uninstallProductBundle(product, BUNDLE_TEST_WAB2);
                    server.uninstallProductBundle(product, BUNDLE_TEST_WAB3);
                    server.uninstallProductFeature(product, product);
                    server.uninstallProductExtension(product);
                }
            }
        }
    }

    @Test
    public void testDefaultConfiguration() throws Exception {
        setConfiguration(CONFIG_DEFAULT);
        checkWAB(PRODUCT1 + SERVLET1, OUTPUT_SERVLET1);
        checkWAB(PRODUCT2 + SERVLET1, OUTPUT_SERVLET1);
    }

    @Test
    public void testMultipleConfiguration() throws Exception {
        setConfiguration(CONFIG_MULTIPLE);
        checkWAB(PRODUCT1 + WAB1 + SERVLET1, OUTPUT_SERVLET1);
        checkWAB(PRODUCT2 + WAB1 + SERVLET1, OUTPUT_SERVLET1);
        checkWAB(PRODUCT1 + WAB2 + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT2 + WAB2 + SERVLET2, OUTPUT_SERVLET2);
    }

    @Test
    public void testConflictConfiguration() throws Exception {
        setConfiguration(CONFIG_CONFLICT);
        // the WAB that won should still be serviceable
        checkWAB(CONFLICT + SERVLET1, OUTPUT_SERVLET1);
    }

    @Test
    public void testNoConfiguredPathRestart() throws Exception {
        setConfiguration(CONFIG_DEFAULT);
        checkWAB(PRODUCT1 + RESTART, OUTPUT_RESTART);
    }

    @Test
    public void testSwitch() throws Exception {
        setConfiguration(CONFIG_DEFAULT);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB2", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB3", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET3, OUTPUT_SERVLET3);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB2", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB3", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET3, OUTPUT_SERVLET3);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB2", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB3", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET3, OUTPUT_SERVLET3);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB2", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB3", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET3, OUTPUT_SERVLET3);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB2", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET2, OUTPUT_SERVLET2);
        checkWAB(PRODUCT1 + SWITCH + "?context=WAB3", OUTPUT_SWITCH);
        checkWAB(SWITCH_TARGET + SERVLET3, OUTPUT_SERVLET3);
    }

    protected void checkWAB(String path, String... expected) throws Exception {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        HttpUtils.findStringInUrl(server, path, expected);
    }

}
