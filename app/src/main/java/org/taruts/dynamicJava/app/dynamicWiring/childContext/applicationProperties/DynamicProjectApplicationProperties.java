package org.taruts.dynamicJava.app.dynamicWiring.childContext.applicationProperties;

public record DynamicProjectApplicationProperties(
        String url,
        String username,
        String password,
        String dynamicInterfacePackage
) {
}
