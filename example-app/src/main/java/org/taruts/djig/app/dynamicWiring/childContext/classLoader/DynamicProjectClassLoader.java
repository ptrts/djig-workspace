package org.taruts.djig.app.dynamicWiring.childContext.classLoader;

import lombok.Getter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Objects;

public class DynamicProjectClassLoader extends URLClassLoader {

    @Getter
    final File classesDirectory;

    @Getter
    final File resourcesDirectory;

    public DynamicProjectClassLoader(File sourceDirectory, File classesDirectory, File resourcesDirectory) {
        super(
                sourceDirectory.getName(),
                filesToUrls(classesDirectory, resourcesDirectory),
                DynamicProjectClassLoader.class.getClassLoader()
        );
        this.classesDirectory = classesDirectory;
        this.resourcesDirectory = resourcesDirectory;
    }

    /**
     * First we look for a resource among those of this class loader
     * and only after that we delegate to the parent.
     * The original implementation {@link ClassLoader#getResource(String)} works in the opposite way.
     * We changed the order this way to enable the dynamic code to override properties in application*.properties files
     * of the main Spring context (of the main class loader).
     */
    @Override
    public URL getResource(String name) {
        Objects.requireNonNull(name);

        // First we try to find the resource among those belonging directly to the class loader
        URL url = findResource(name);

        // Then we delegate to the parent
        if (url == null) {
            ClassLoader parent = getParent();
            if (parent != null) {
                url = parent.getResource(name);
            }
        }

        return url;
    }

    private static URL[] filesToUrls(File... directories) {
        return Arrays
                .stream(directories)
                .map(DynamicProjectClassLoader::fileToUrl)
                .toArray(URL[]::new);
    }

    private static URL fileToUrl(File directory) {
        try {
            return UriComponentsBuilder
                    .fromUri(directory.toURI())
                    .scheme("file")
                    .build()
                    .toUri()
                    .toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
