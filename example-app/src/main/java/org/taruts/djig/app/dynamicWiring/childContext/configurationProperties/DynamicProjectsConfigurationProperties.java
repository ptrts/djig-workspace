package org.taruts.djig.app.dynamicWiring.childContext.configurationProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;

@ConfigurationProperties(DynamicProjectsConfigurationProperties.PREFIX)
public class DynamicProjectsConfigurationProperties extends HashMap<String, DynamicProjectConfigurationProperties> {
    public static final String PREFIX = "app.dynamic-projects";
}
