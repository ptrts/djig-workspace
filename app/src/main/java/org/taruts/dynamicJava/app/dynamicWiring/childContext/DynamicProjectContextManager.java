package org.taruts.dynamicJava.app.dynamicWiring.childContext;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProject;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.classLoader.DynamicProjectClassLoader;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.context.GradleProjectApplicationContext;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.gradleBuild.DynamicProjectGradleBuild;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.gradleBuild.DynamicProjectGradleBuildService;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.remote.DynamicProjectCloner;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.source.DynamicProjectLocalGitRepo;
import org.taruts.dynamicJava.app.dynamicWiring.mainContext.proxy.DynamicComponentProxy;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@Slf4j
public class DynamicProjectContextManager {

    @Autowired
    private ApplicationContext mainContext;

    @Autowired
    private DynamicProjectCloner dynamicProjectGitRemoteService;

    @Autowired
    private DynamicProjectGradleBuildService dynamicProjectGradleBuildService;

    @SneakyThrows
    public void init(DynamicProject dynamicProject) {
        File sourceDirectory = dynamicProject.getSourceDirectory();
        if (sourceDirectory.exists()) {
            FileUtils.forceDelete(sourceDirectory);
        }
        refresh(dynamicProject);
    }

    public void refresh(DynamicProject dynamicProject) {
        GradleProjectApplicationContext newChildContext = createNewChildContext(dynamicProject);
        setNewDelegatesInMainContext(dynamicProject, newChildContext);
        closeOldChildContextAndSetNewReference(dynamicProject, newChildContext);
    }

    private GradleProjectApplicationContext createNewChildContext(DynamicProject dynamicProject) {

        // Clone
        DynamicProjectLocalGitRepo dynamicProjectLocalGitRepo = dynamicProjectGitRemoteService.cloneWithRetries(
                dynamicProject.getRemote(),
                dynamicProject.getSourceDirectory()
        );

        // Build
        DynamicProjectGradleBuild build = dynamicProjectGradleBuildService.build(dynamicProjectLocalGitRepo);

        DynamicProjectClassLoader childClassLoader = new DynamicProjectClassLoader(
                dynamicProject.getSourceDirectory(),
                build.classesDirectory(),
                build.resourcesDirectory()
        );

        GradleProjectApplicationContext newContext = new GradleProjectApplicationContext(mainContext, childClassLoader);

        newContext.refresh();
        return newContext;
    }

    private void setNewDelegatesInMainContext(DynamicProject dynamicProject, GradleProjectApplicationContext newContext) {

        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) mainContext).getBeanFactory();

        String thisProjectProxiesQualifier = dynamicProject.getName();

        @SuppressWarnings("rawtypes")
        Map<String, DynamicComponentProxy> proxyBeansMap = BeanFactoryAnnotationUtils.qualifiedBeansOfType(
                beanFactory,
                DynamicComponentProxy.class,
                thisProjectProxiesQualifier
        );

        proxyBeansMap.forEach((proxyBeanName, proxy) -> {

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

    private void closeOldChildContextAndSetNewReference(DynamicProject project, GradleProjectApplicationContext newChildContext) {
        // Closing the old context
        GradleProjectApplicationContext oldChildContext = project.getContext();
        if (oldChildContext != null) {
            oldChildContext.close();
        }

        // Saving the new context
        project.setContext(newChildContext);
    }
}
