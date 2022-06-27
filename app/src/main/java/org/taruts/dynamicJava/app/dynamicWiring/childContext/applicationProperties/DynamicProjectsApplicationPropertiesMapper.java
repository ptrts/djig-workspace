package org.taruts.dynamicJava.app.dynamicWiring.childContext.applicationProperties;

import com.google.common.base.Functions;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProject;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.remote.DynamicProjectGitRemote;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class DynamicProjectsApplicationPropertiesMapper {

    public static Map<String, DynamicProject> map(DynamicProjectsApplicationProperties dynamicProjectsApplicationProperties) {
        return dynamicProjectsApplicationProperties
                .entrySet()
                .stream()
                .map(entry -> {
                    String dynamicProjectName = entry.getKey();
                    DynamicProjectApplicationProperties dynamicProjectApplicationProperties = entry.getValue();
                    return map(dynamicProjectName, dynamicProjectApplicationProperties);
                })
                .collect(Collectors.toMap(DynamicProject::getName, Functions.identity()));
    }

    private static DynamicProject map(String dynamicProjectName, DynamicProjectApplicationProperties dynamicProjectApplicationProperties) {
        DynamicProjectGitRemote remote = new DynamicProjectGitRemote(
                dynamicProjectApplicationProperties.url(),
                dynamicProjectApplicationProperties.username(),
                dynamicProjectApplicationProperties.password()
        );
        File sourceDirectory = Path.of(dynamicProjectName).toAbsolutePath().normalize().toFile();
        return new DynamicProject(
                dynamicProjectName,
                remote,
                sourceDirectory,
                dynamicProjectApplicationProperties.dynamicInterfacePackage()
        );
    }
}
