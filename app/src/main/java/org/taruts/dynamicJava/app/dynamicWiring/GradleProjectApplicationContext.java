package org.taruts.dynamicJava.app.dynamicWiring;

import lombok.SneakyThrows;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.taruts.gitUtils.GitUtils;
import org.taruts.gradleUtils.GradleBuilder;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class GradleProjectApplicationContext extends AnnotationConfigApplicationContext {

    private final RetryTemplate cloneRetryTemplate = new RetryTemplate();

    public GradleProjectApplicationContext(
            ApplicationContext parent,
            String urlArg,
            String username,
            String password,
            File projectSourceDirectory
    ) {
        boolean directoryUrl = Stream.of(
                "http:", "https:", "file:", "git@"
        ).noneMatch(urlArg::startsWith);

        String url;
        if (directoryUrl) {
            // Во-первых, ко всем файловым URL без file:// в начале мы будем добавлять file://
            // Это нужно для того, чтобы клонирование осуществлялось не копированием файлов, а тем же самым кодом в Git, который используется для сетевых URL
            // Соответственно, здесь мы также должны делать из относительных путей абсолютные
            url = "file://" + Path.of(urlArg).toAbsolutePath().normalize();
        } else {
            url = urlArg;
        }

        cloneRetryTemplate.execute(retryContext -> {
            GitUtils.cloneOrUpdate(url, username, password, projectSourceDirectory);
            return null;
        });

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
        String classLoaderName = projectSourceDirectory.getName();
        ClassLoader classLoader = createClassLoader(classLoaderName, classesDirectory, resourcesDirectory);

        // Setting ClassLoader
        setClassLoader(classLoader);
        getBeanFactory().setBeanClassLoader(classLoader);

        // Setting environment
        ConfigurableEnvironment environment = new AbstractEnvironment() {
        };
        setEnvironment(environment);

        // Здесь родительский Environment будет подмержен к нашему Environment
        setParent(parent);

        tweakEnvironment(environment, classLoader);

        // Scan the root package of the project
        String rootPackageName = getRootPackageName(classesDirectory);
        scan(rootPackageName);
    }

    @PostConstruct
    private void init() {
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(0L);
        cloneRetryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        cloneRetryTemplate.setRetryPolicy(retryPolicy);
    }

    private void tweakEnvironment(ConfigurableEnvironment environment, ClassLoader classLoader) {

        MutablePropertySources propertySources = environment.getPropertySources();

        // Получаем список объектов PropertySource для файлов типа application-dev.properties или application.properties
        MutablePropertySources applicationPropertiesPropertySources = getApplicationPropertiesPropertySources(classLoader, environment.getActiveProfiles());

        // Вставляем их куда надо в нашу пачку из PropertySource

        // Ищем имя PropertySource, после которого мы будем делать вставку своих
        String nameToAddAfter = Stream
                .of("systemProperties", "systemEnvironment")
                .map(propertySources::get)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(propertySources::precedenceOf))
                .map(PropertySource::getName)
                .orElseThrow(() -> new IllegalStateException("Neither of property sources systemProperties or systemEnvironment is found"));

        // Делаем вставку
        for (PropertySource<?> applicationPropertiesPropertySource : applicationPropertiesPropertySources) {
            String name = "Child: " + applicationPropertiesPropertySource.getName();
            PropertySource<?> propertySource = new ProxyPropertySource(name, applicationPropertiesPropertySource);
            propertySources.addAfter(nameToAddAfter, propertySource);
            nameToAddAfter = name;
        }

        // PropertySource configurationProperties выглядит как лишняя деталь
        // Он содержит внутри себя тот же самый список из объектов PropertySource, в котором сидит сам
        // Чем создавать такой же как в родительском контексте здесь у себя, пусть лучше у нас его просто не будет
        propertySources.remove("configurationProperties");
    }

    private MutablePropertySources getApplicationPropertiesPropertySources(ClassLoader classLoader, String[] activeProfiles) {
        ConfigurableEnvironment environment = new AbstractEnvironment() {
        };
        environment.setActiveProfiles(activeProfiles);
        ResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);
        ConfigDataEnvironmentPostProcessor.applyTo(environment, resourceLoader, null);
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

    @SneakyThrows
    private ClassLoader createClassLoader(String classLoaderName, File classesDirectory, File resourcesDirectory) {

        URL classesUrl = UriComponentsBuilder
                .fromUri(classesDirectory.toURI())
                .scheme("file")
                .build()
                .toUri()
                .toURL();

        URL resourcesUrl = UriComponentsBuilder
                .fromUri(resourcesDirectory.toURI())
                .scheme("file")
                .build()
                .toUri()
                .toURL();

        return new OurUrlClassLoader(
                classLoaderName,
                new URL[]{
                        classesUrl,
                        resourcesUrl
                },
                getClass().getClassLoader()
        );
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
