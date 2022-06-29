package org.taruts.djig.app.dynamicWiring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.taruts.djig.app.dynamicWiring.childContext.context.GradleProjectApplicationContext;
import org.taruts.djig.app.dynamicWiring.childContext.remote.DynamicProjectGitRemote;

import java.io.File;

@RequiredArgsConstructor
@Getter
@Setter
public class DynamicProject {
    final String name;
    final DynamicProjectGitRemote remote;
    final File sourceDirectory;
    final String dynamicInterfacePackage;
    GradleProjectApplicationContext context;
}