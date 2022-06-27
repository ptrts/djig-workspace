package org.taruts.dynamicJava.app.dynamicWiring.childContext.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.classLoader.DynamicProjectClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class GradleProjectApplicationContext extends AnnotationConfigApplicationContext {

    public GradleProjectApplicationContext(
            ApplicationContext parent,
            DynamicProjectClassLoader childClassLoader
    ) {
        // Setting ClassLoader
        setClassLoader(childClassLoader);
        getBeanFactory().setBeanClassLoader(childClassLoader);

        registerBean(DynamicComponentDefinitionPostProcessor.class, childClassLoader);

        // Setting the simplest environment, which won't add any PropertySources
        setEnvironment(new AbstractEnvironment() {
        });

        // Setting the parent.
        // This also merges the parent Environment into the child context Environment.
        setParent(parent);

        enableApplicationPropertiesOverriding();

        // Scan the root package of the project
        String rootPackageName = getLongestPackagePrefix(childClassLoader.getClassesDirectory());
        scan(rootPackageName);
    }

    /**
     * Add {@link PropertySource}s for application*.properties files of the dynamic project
     */
    private void enableApplicationPropertiesOverriding() {

        ClassLoader classLoader = getClassLoader();
        ConfigurableEnvironment environment = getEnvironment();

        MutablePropertySources propertySources = environment.getPropertySources();

        // Getting a list of PropertySource for files like application-dev.properties and application.properties
        MutablePropertySources applicationPropertiesPropertySources = getApplicationPropertiesPropertySources(
                classLoader,
                environment.getActiveProfiles()
        );

        // Inserting them in the environment

        // Determining the position of insertion.
        // The point is to put property sources loaded from the child class loader before those from the main context.
        // We'll do the insertion after systemProperties or systemEnvironment whichever goes last.
        String nameToAddAfter = Stream
                .of("systemProperties", "systemEnvironment")
                .map(propertySources::get)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(propertySources::precedenceOf))
                .map(PropertySource::getName)
                .orElseThrow(() -> new IllegalStateException("Neither of property sources systemProperties or systemEnvironment is found"));

        // Performing the insertion
        for (PropertySource<?> applicationPropertiesPropertySource : applicationPropertiesPropertySources) {
            String name = "Child: " + applicationPropertiesPropertySource.getName();
            PropertySource<?> propertySource = new DelegatingPropertySource(name, applicationPropertiesPropertySource);
            propertySources.addAfter(nameToAddAfter, propertySource);
            nameToAddAfter = name;
        }

        // The PropertySource configurationProperties looks like a useless component in all this.
        // It contains the same list of PropertySource objects it itself sits in.
        propertySources.remove("configurationProperties");
    }

    /**
     * Build standard {@link MutablePropertySources} of Spring Boot according to the active profiles and application*.properties files
     * on the class path.
     * <p>
     * Properties in application*.properties files in the dynamic project override properties of such files in the main application.
     * <p>
     * Again. It's not files overriding files. It's properties overriding properties.
     * <p>
     * Properties from the main application-dev.properties stay if they are not specified in the dynamic file with that name.
     */
    private MutablePropertySources getApplicationPropertiesPropertySources(ClassLoader classLoader, String[] activeProfiles) {

        // We will use a temporary Environment object here
        ConfigurableEnvironment environment = new AbstractEnvironment() {
        };
        environment.setActiveProfiles(activeProfiles);

        // We use the standard Spring Boot ConfigDataEnvironmentPostProcessor to populate the temporary Environment
        // with standard Spring Boot configuration property sources.
        // The property sources will include those for profile .property and YAML files.
        // The configuration property files might come from the main application (main class loader) or be overridden in the
        // dynamic project (child class loader).
        ResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
        ConfigDataEnvironmentPostProcessor.applyTo(environment, resourceLoader, null);

        // Return the populated property sources in the temporary Environment
        return environment.getPropertySources();
    }

    /**
     * @param classesDirectory A directory with hierarchical package subdirectories and .class files inside them
     * @return The longest package prefix which all classes in {@code classesDirectory} share.
     * This corresponds to the topmost package subdirectory with a .class file or with more than two subdirectories inside.
     */
    private String getLongestPackagePrefix(File classesDirectory) {
        List<String> prefixNameParts = new ArrayList<>();
        File currentDirectory = classesDirectory;
        while (true) {
            File[] childFiles = currentDirectory.listFiles();
            if (childFiles == null) {
                break;
            }

            boolean someChildrenAreClasses = Stream
                    .of(childFiles)
                    .filter(File::isFile)
                    .map(File::getName)
                    .anyMatch(name -> name.endsWith(".class"));
            if (someChildrenAreClasses) {
                break;
            }

            List<File> childDirectories = Stream
                    .of(childFiles)
                    .filter(File::isDirectory)
                    .toList();
            if (childDirectories.size() != 1) {
                // Either this subdirectory has more than two subdirectories inside or it is completely empty.
                // We stop the search in both cases.
                break;
            }

            // If we are here, then there is just one single subdirectory inside and no .class files.
            // So all the .class files are further down, and all will have the subdirectory name in their package.
            // We add this directory to the prefix and proceed further down.
            currentDirectory = childDirectories.get(0);
            prefixNameParts.add(currentDirectory.getName());
        }
        return String.join(".", prefixNameParts);
    }
}
