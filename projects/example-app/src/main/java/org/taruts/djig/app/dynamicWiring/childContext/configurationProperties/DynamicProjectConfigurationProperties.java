package org.taruts.djig.app.dynamicWiring.childContext.configurationProperties;

public record DynamicProjectConfigurationProperties(
        String url,
        String username,
        String password,
        String dynamicInterfacePackage
) {
}
