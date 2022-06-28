package org.taruts.dynamicJava.app.dynamicWiring.childContext.source;

import java.io.File;
import java.nio.file.Path;

public class DynamicProjectSourceLocator {
    public static File getSourceDirectory(String dynamicProjectName) {
        return Path.of(dynamicProjectName).toAbsolutePath().normalize().toFile();
    }
}
