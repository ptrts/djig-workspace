package org.taruts.dynamicJava.app.dynamicWiring.childContext.gradleBuild;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.source.DynamicProjectLocalGitRepo;
import org.taruts.gradleUtils.GradleBuilder;

import java.io.File;

@Component
public class DynamicProjectGradleBuildService {

    public DynamicProjectGradleBuild build(DynamicProjectLocalGitRepo localGitRepo) {

        File projectSourceDirectory = localGitRepo.directory();

        GradleBuilder.buildGradleProject(projectSourceDirectory);

        File classesDirectory = FileUtils.getFile(projectSourceDirectory, "build/classes/java/main");
        if (!classesDirectory.exists() || !classesDirectory.isDirectory()) {
            throw new IllegalStateException();
        }

        File resourcesDirectory = FileUtils.getFile(projectSourceDirectory, "build/resources/main");
        if (!resourcesDirectory.exists() || !resourcesDirectory.isDirectory()) {
            throw new IllegalStateException();
        }

        return new DynamicProjectGradleBuild(localGitRepo, classesDirectory, resourcesDirectory);
    }
}
