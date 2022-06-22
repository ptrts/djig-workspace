package org.taruts.dynamicJava.app.dynamicWiring;

import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Objects;

public class OurUrlClassLoader extends URLClassLoader {

    public OurUrlClassLoader(String name, ClassLoader parent, File... directories) {
        super(name, filesToUrls(directories), parent);
    }

    /**
     * First we look for a resource among those of this class loader
     * and only after that we delegate to the parent.
     * The original implementation works in the opposite way.
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
                .map(OurUrlClassLoader::fileToUrl)
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
