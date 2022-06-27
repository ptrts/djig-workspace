package org.taruts.dynamicJava.app.dynamicWiring.childContext;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProjectProperties;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.classLoader.DynamicProjectClassLoader;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.context.GradleProjectApplicationContext;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.gradleBuild.DynamicProjectGradleBuild;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.gradleBuild.DynamicProjectGradleBuildService;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.remote.DynamicProjectCloner;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.remote.DynamicProjectGitRemote;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.source.DynamicProjectLocalGitRepo;
import org.taruts.dynamicJava.app.dynamicWiring.mainContext.proxy.DynamicComponentProxy;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@Slf4j
public class GradleProjectApplicationContextContainer {

    @Autowired
    private ApplicationContext mainContext;

    private final Map<String, GradleProjectApplicationContext> childContexts = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    private DynamicProjectCloner dynamicProjectGitRemoteService;

    @Autowired
    private DynamicProjectGradleBuildService dynamicProjectGradleBuildService;

    @SneakyThrows
    public void init(String projectName, DynamicProjectProperties dynamicProjectProperties) {
        File localGitRepoDirectory = dynamicProjectProperties.getLocalGitRepoDirectory();
        if (localGitRepoDirectory.exists()) {
            FileUtils.forceDelete(localGitRepoDirectory);
        }
        refresh(projectName, dynamicProjectProperties);
    }

    public void refresh(String projectName, DynamicProjectProperties dynamicProjectProperties) {
        GradleProjectApplicationContext newChildContext = createNewChildContext(projectName, dynamicProjectProperties);
        setNewDelegatesInMainContext(projectName, newChildContext);
        closeOldChildContextAndSetNewReference(projectName, newChildContext);
    }

    private GradleProjectApplicationContext createNewChildContext(String projectName, DynamicProjectProperties dynamicProjectProperties) {

        DynamicProjectGitRemote remote = new DynamicProjectGitRemote(
                dynamicProjectProperties.getUrl(),
                dynamicProjectProperties.getUsername(),
                dynamicProjectProperties.getPassword()
        );

        File sourceDirectory = Path.of(projectName).toAbsolutePath().normalize().toFile();

        // Clone
        DynamicProjectLocalGitRepo dynamicProjectLocalGitRepo = dynamicProjectGitRemoteService.cloneWithRetries(remote, sourceDirectory);

        // Build
        DynamicProjectGradleBuild build = dynamicProjectGradleBuildService.build(dynamicProjectLocalGitRepo);

        DynamicProjectClassLoader childClassLoader = new DynamicProjectClassLoader(
                sourceDirectory, build.classesDirectory(), build.resourcesDirectory());

        GradleProjectApplicationContext newContext = new GradleProjectApplicationContext(
                mainContext,
                childClassLoader
        );

        newContext.refresh();
        return newContext;
    }

    private void setNewDelegatesInMainContext(String projectName, GradleProjectApplicationContext newContext) {

        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) mainContext).getBeanFactory();

        @SuppressWarnings("rawtypes")
        Map<String, DynamicComponentProxy> proxiesMap = BeanFactoryAnnotationUtils.qualifiedBeansOfType(
                beanFactory,
                DynamicComponentProxy.class,
                projectName
        );

        proxiesMap.forEach((proxyBeanName, proxy) -> {

            Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();

            Class<? extends DynamicComponent> dynamicProxyInterface = Stream
                    .of(proxyInterfaces)
                    .filter(DynamicComponent.class::isAssignableFrom)
                    .map(iface -> {
                        //noinspection unchecked
                        return (Class<? extends DynamicComponent>) iface;
                    })
                    .findAny()
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "A DynamicComponentProxy must implement one of the interfaces extending DynamicComponent"
                            )
                    );

            Map<String, ? extends DynamicComponent> dynamicImplementationsMap = newContext.getBeansOfType(dynamicProxyInterface);

            List<? extends DynamicComponent> implementations = dynamicImplementationsMap
                    .values()
                    .stream()
                    .filter(currentImplementation -> !(currentImplementation instanceof DynamicComponentProxy))
                    .toList();
            DynamicComponent childContextDynamicImplementation = implementations.get(0);

            //noinspection unchecked
            proxy.setDelegate(childContextDynamicImplementation);
        });
    }

    private void closeOldChildContextAndSetNewReference(String projectName, GradleProjectApplicationContext newChildContext) {

        // Closing the old context
        GradleProjectApplicationContext childContext = childContexts.get(projectName);
        if (childContext != null) {
            childContext.close();
        }

        // Saving the new context in the field
        childContexts.put(projectName, newChildContext);
    }

    @EventListener(ContextClosedEvent.class)
    public void close(ContextClosedEvent event) {

        if (event.getApplicationContext() != mainContext) {
            return;
        }

        List<String> keys = new ArrayList<>(childContexts.keySet());
        keys.forEach(projectName ->
                childContexts.computeIfPresent(projectName, (projectName2, childContext) -> {
                    childContext.close();
                    return null;
                })
        );
    }
}
