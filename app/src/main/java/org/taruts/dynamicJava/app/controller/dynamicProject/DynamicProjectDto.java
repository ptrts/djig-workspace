package org.taruts.dynamicJava.app.controller.dynamicProject;

public record DynamicProjectDto(
        String url,
        String username,
        String password,
        String dynamicInterfacePackage
) {
}
