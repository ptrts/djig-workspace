package org.taruts.dynamicJava.app.dynamicWiring.childContext.source;

import org.taruts.dynamicJava.app.dynamicWiring.childContext.remote.DynamicProjectGitRemote;

import java.io.File;

public record DynamicProjectLocalGitRepo(DynamicProjectGitRemote remote, File directory) {
}
