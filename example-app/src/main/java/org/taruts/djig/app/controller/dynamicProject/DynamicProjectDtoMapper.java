package org.taruts.djig.app.controller.dynamicProject;

import org.taruts.djig.app.dynamicWiring.DynamicProject;
import org.taruts.djig.app.dynamicWiring.childContext.remote.DynamicProjectGitRemote;
import org.taruts.djig.app.dynamicWiring.childContext.source.DynamicProjectSourceLocator;

import java.io.File;

public class DynamicProjectDtoMapper {
    public static DynamicProject map(String projectName, DynamicProjectDto dto) {
        DynamicProjectGitRemote remote = new DynamicProjectGitRemote(
                dto.url(),
                dto.username(),
                dto.password()
        );

        File sourceDirectory = DynamicProjectSourceLocator.getSourceDirectory(projectName);

        return new DynamicProject(
                projectName,
                remote,
                sourceDirectory,
                dto.dynamicInterfacePackage()
        );
    }
}