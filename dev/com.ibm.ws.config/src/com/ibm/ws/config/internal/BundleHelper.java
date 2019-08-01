package com.ibm.ws.config.internal;

import org.apache.felix.scr.component.manager.ComponentManager;
import org.apache.felix.scr.component.manager.ComponentManagerFactory;

import com.ibm.ws.config.featuregen.internal.FeatureListComponentManager;
import com.ibm.ws.config.featuregen.internal.FeatureListMBeanImpl;
import com.ibm.ws.config.schemagen.internal.SchemaGeneratorComponentManager;
import com.ibm.ws.config.schemagen.internal.SchemaGeneratorImpl;
import com.ibm.ws.config.schemagen.internal.ServerSchemaGeneratorComponentManager;
import com.ibm.ws.config.schemagen.internal.ServerSchemaGeneratorImpl;
import com.ibm.ws.config.xml.internal.ConfigIntrospection;
import com.ibm.ws.config.xml.internal.ConfigIntrospectionComponentManager;
import com.ibm.ws.config.xml.internal.MetaTypeIntrospection;
import com.ibm.ws.config.xml.internal.MetaTypeIntrospectionComponentManager;
import com.ibm.ws.config.xml.internal.ServerConfigMBeanComponentManager;
import com.ibm.ws.config.xml.internal.ServerXMLConfigurationMBeanImpl;
import com.ibm.ws.config.xml.internal.metatype.MetaTypeFactoryComponentManager;
import com.ibm.ws.config.xml.internal.metatype.MetaTypeFactoryImpl;

public class BundleHelper implements ComponentManagerFactory {

    @Override
    public ComponentManager createComponentManager(String componentName) {
        if (MetaTypeFactoryImpl.class.getName().equals(componentName)) {
            return new MetaTypeFactoryComponentManager();
        }
        if (FeatureListMBeanImpl.class.getName().equals(componentName)) {
            return new FeatureListComponentManager();
        }
        if (ServerSchemaGeneratorImpl.class.getName().equals(componentName)) {
            return new ServerSchemaGeneratorComponentManager();
        }
        if (SchemaGeneratorImpl.class.getName().equals(componentName)) {
            return new SchemaGeneratorComponentManager();
        }
        if (ConfigIntrospection.class.getName().equals(componentName)) {
            return new ConfigIntrospectionComponentManager();
        }
        if (MetaTypeIntrospection.class.getName().equals(componentName)) {
            return new MetaTypeIntrospectionComponentManager();
        }
        if (ServerXMLConfigurationMBeanImpl.class.getName().equals(componentName)) {
            return new ServerConfigMBeanComponentManager();
        }
        return null;
    }
}