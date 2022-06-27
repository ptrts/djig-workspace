package org.taruts.dynamicJava.app.dynamicWiring.childContext.gradleBuild;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.taruts.gradleUtils.GradleBuilder;

import java.io.File;

@Component
public class DynamicProjectGradleBuildService {

    public DynamicProjectGradleBuild build(File projectSourceDirectory) {

        GradleBuilder.buildGradleProject(projectSourceDirectory);

        File classesDirectory = FileUtils.getFile(projectSourceDirectory, "build/classes/java/main");
        if (!classesDirectory.exists() || !classesDirectory.isDirectory()) {
            throw new IllegalStateException();
        }

        File resourcesDirectory = FileUtils.getFile(projectSourceDirectory, "build/resources/main");
        if (!resourcesDirectory.exists() || !resourcesDirectory.isDirectory()) {
            throw new IllegalStateException();
        }

        return new DynamicProjectGradleBuild(classesDirectory, resourcesDirectory);
    }
}
