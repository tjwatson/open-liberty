/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 *
 */
public final class SpringConstants {
    public static final String SPRING_APP_TYPE = "spr";
    public static final String SPRING_BOOT_SUPPORT_CAPABILITY = "spring.boot.support";
    public static final String SPRING_BOOT_SUPPORT_CAPABILITY_JARS = "jars";
    public static final String SPRING_START_CLASS_HEADER = "Start-Class";
    public static final String SPRING_BOOT_CLASSES_HEADER = "Spring-Boot-Classes";
    public static final String SPRING_BOOT_LIB_HEADER = "Spring-Boot-Lib";
    public static final String SPRING_SHARED_LIB_CACHE_DIR = "lib.index.cache/";
    public static final String SPRING_LIB_INDEX_FILE = "META-INF/spring.lib.index";
    public static final String SPRING_THIN_APPS_DIR = WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + "apps/spring.thin.apps/";
}
