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
package com.ibm.ws.jca.fat.classloading.cdilib;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A CDI bean in a shared library that can be injected into applications.
 */
@ApplicationScoped
public class CDIBean {

    public static final String MESSAGE = "CDI Bean from Library";

    public String getMessage() {
        return MESSAGE;
    }

    public String processData(String input) {
        return "Processed by CDI Bean: " + input;
    }
}
