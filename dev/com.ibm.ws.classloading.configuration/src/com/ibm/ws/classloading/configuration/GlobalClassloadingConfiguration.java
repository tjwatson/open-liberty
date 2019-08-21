/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.configuration;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

@Component(service = GlobalClassloadingConfiguration.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = "com.ibm.ws.classloading.global", property = "service.vendor=IBM")
public class GlobalClassloadingConfiguration {

    private Map<String, Object> properties;

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext cCtx) {

        this.properties = (Map<String, Object>) cCtx.getProperties();
    }

    @Deactivate
    protected void deactivate(ComponentContext cCtx) {
        properties = null;
    }

    @SuppressWarnings("unchecked")
    @Modified
    protected void modified(ComponentContext ctx) {
        this.properties = (Map<String, Object>) ctx.getProperties();
    }

    /**
     * @return
     */
    public boolean useJarUrls() {
        return properties == null || (Boolean) properties.get("useJarUrls");
    }

}
