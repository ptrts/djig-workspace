package org.taruts.dynamicJava.app.dynamicWiring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;

@ConfigurationProperties(DynamicProjectsProperties.PREFIX)
public class DynamicProjectsProperties extends HashMap<String, DynamicProjectProperties> {
    public static final String PREFIX = "app.dynamic-projects";
}
