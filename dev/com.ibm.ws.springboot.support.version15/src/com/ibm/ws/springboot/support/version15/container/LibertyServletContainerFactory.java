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
package com.ibm.ws.springboot.support.version15.container;

import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 *
 */
public class LibertyServletContainerFactory extends AbstractEmbeddedServletContainerFactory {

    /* (non-Javadoc)
     * @see org.springframework.boot.context.embedded.EmbeddedServletContainerFactory#getEmbeddedServletContainer(org.springframework.boot.web.servlet.ServletContextInitializer[])
     */
    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}
