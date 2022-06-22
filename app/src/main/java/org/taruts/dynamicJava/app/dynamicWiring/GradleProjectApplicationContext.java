package org.taruts.dynamicJava.app.dynamicWiring;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.taruts.dynamicJava.app.dynamicWiring.dynamicComponent.DynamicComponentDefinitionPostProcessor;
import org.taruts.gitUtils.GitUtils;
import org.taruts.gradleUtils.GradleBuilder;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class GradleProjectApplicationContext extends AnnotationConfigApplicationContext {

    public GradleProjectApplicationContext(
            ApplicationContext parent,
            String url,
            String username,
            String password,
            File projectSourceDirectory
    ) {

        // todo Вытащить сие из контекста?
        //      Может сделать так, чтобы контекст делался уже из готового class loader?

        cloneWithRetries(username, password, projectSourceDirectory, url);

        GradleBuilder.buildGradleProject(projectSourceDirectory);

        File classesDirectory = FileUtils.getFile(projectSourceDirectory, "build/classes/java/main");
        if (!classesDirectory.exists() || !classesDirectory.isDirectory()) {
            throw new IllegalStateException();
        }

        File resourcesDirectory = FileUtils.getFile(projectSourceDirectory, "build/resources/main");
        if (!resourcesDirectory.exists() || !resourcesDirectory.isDirectory()) {
            throw new IllegalStateException();
        }

        // Create ClassLoader
        ClassLoader classLoader = createClassLoader(projectSourceDirectory, classesDirectory, resourcesDirectory);

        // Setting ClassLoader
        setClassLoader(classLoader);
        getBeanFactory().setBeanClassLoader(classLoader);

        registerBean(DynamicComponentDefinitionPostProcessor.class, classLoader);

        // Setting environment
        setEnvironment(new AbstractEnvironment() {
        });

        // Setting the parent.
        // This also merges the parent Environment into the child context Environment.
        setParent(parent);

        enableApplicationPropertiesOverriding();

        // Scan the root package of the project
        String rootPackageName = getRootPackageName(classesDirectory);
        scan(rootPackageName);
    }

    private void cloneWithRetries(String username, String password, File projectSourceDirectory, String urlArg) {
        String url = tweakLocalDirectoryRemoteUrl(urlArg);
        RetryTemplate cloneRetryTemplate = getCloneRetryTemplate();
        cloneRetryTemplate.execute(retryContext -> {
            GitUtils.cloneOrUpdate(url, username, password, projectSourceDirectory);
            return null;
        });
    }

    private String tweakLocalDirectoryRemoteUrl(String urlArg) {
        boolean isRemoteLocalDirectoryPath = Stream.of(
                "http:", "https:", "file:", "git@"
        ).noneMatch(urlArg::startsWith);

        if (isRemoteLocalDirectoryPath) {
            // Add "file://" so that cloning would be done in a similar way as with remotes on the network.
            // Plus, transform the path from relative to absolute.
            return "file://" + Path.of(urlArg).toAbsolutePath().normalize();
        } else {
            return urlArg;
        }
    }

    private RetryTemplate getCloneRetryTemplate() {
        RetryTemplate cloneRetryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(0L);
        cloneRetryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        cloneRetryTemplate.setRetryPolicy(retryPolicy);

        return cloneRetryTemplate;
    }

    private ClassLoader createClassLoader(File projectSourceDirectory, File classesDirectory, File resourcesDirectory) {
        String classLoaderName = projectSourceDirectory.getName();
        return new OurUrlClassLoader(
                classLoaderName,
                getClass().getClassLoader(),
                classesDirectory,
                resourcesDirectory
        );
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

        // Determining the position of insertion
        // The point is to put property sources loaded from the child class loader before those from the main context
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
            PropertySource<?> propertySource = new ProxyPropertySource(name, applicationPropertiesPropertySource);
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

    private String getRootPackageName(File classesDirectory) {
        List<String> rootPackageNameParts = new ArrayList<>();
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
                break;
            }
            currentDirectory = childDirectories.get(0);

            rootPackageNameParts.add(currentDirectory.getName());
        }
        return String.join(".", rootPackageNameParts);
    }
}

class ProxyPropertySource extends PropertySource<PropertySource<?>> {

    public ProxyPropertySource(String name, PropertySource source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return source.getProperty(name);
    }
}
