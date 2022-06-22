package org.taruts.dynamicJava.app.dynamicWiring.childContext.gradleBuild;

import org.taruts.dynamicJava.app.dynamicWiring.childContext.source.DynamicProjectLocalGitRepo;

import java.io.File;

public record DynamicProjectGradleBuild(DynamicProjectLocalGitRepo localGitRepo, File classesDirectory, File resourcesDirectory) {
}
