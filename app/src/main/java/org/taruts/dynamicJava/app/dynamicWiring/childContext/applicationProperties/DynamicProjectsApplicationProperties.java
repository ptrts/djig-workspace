package org.taruts.dynamicJava.app.dynamicWiring.childContext.applicationProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;

@ConfigurationProperties(DynamicProjectsApplicationProperties.PREFIX)
public class DynamicProjectsApplicationProperties extends HashMap<String, DynamicProjectApplicationProperties> {
    public static final String PREFIX = "app.dynamic-projects";
}
