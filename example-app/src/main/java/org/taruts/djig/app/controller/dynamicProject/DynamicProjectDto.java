package org.taruts.djig.app.controller.dynamicProject;

public record DynamicProjectDto(
        String url,
        String username,
        String password,
        String dynamicInterfacePackage
) {
}
