package org.taruts.dynamicJava.app.dynamicWiring;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

public class OurUrlClassLoader extends URLClassLoader {

    public OurUrlClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name, urls, parent);
    }

    @Override
    public URL getResource(String name) {
        Objects.requireNonNull(name);

        URL url = findResource(name);

        if (url == null) {
            ClassLoader parent = getParent();
            if (parent != null) {
                url = parent.getResource(name);
            }
        }

        return url;
    }
}
