/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.atomos;

import java.io.File;
import java.util.Map;

import org.atomos.framework.AtomosRuntime;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.Launcher;
import com.ibm.ws.kernel.boot.LocationException;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.KernelUtils;

/**
 *
 */
public class Liberty {
    public static void main(String[] args) {

        File lib = new File("lib").getAbsoluteFile();
        if (lib.exists()) {
            // set some statics for lib
            KernelUtils.setBootStrapLibDir(lib);
            Utils.setInstallDir(lib.getParentFile());
        }

        Launcher launcher = new Launcher() {
            @Override
            protected BootstrapConfig createBootstrapConfig() {
                BootstrapConfig config = new BootstrapConfig() {
                    @Override
                    protected void configure(Map<String, String> initProps) throws LocationException {
                        initProps.put(AtomosRuntime.ATOMOS_BUNDLE_INSTALL, "false");
                        initProps.put(AtomosRuntime.ATOMOS_BUNDLE_START, "false");
                        initProps.put(BootstrapConstants.LIBERTY_BOOT_PROPERTY, "true");
                        super.configure(initProps);
                    }
                };
                config.setAtomosRuntime(AtomosRuntime.newAtomosRuntime());
                return config;
            }
        };
        System.exit(launcher.createPlatform(args));
    }
}
