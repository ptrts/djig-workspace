package org.taruts.dynamicJava.app.dynamicWiring;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.dynamicComponent.proxy.DynamicComponentProxy;
import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class GradleProjectApplicationContextContainer {

    @Autowired
    private ApplicationContext mainContext;

    private GradleProjectApplicationContext childContext;

    @Autowired
    private DynamicImplProperties dynamicImplProperties;

    @Autowired
    @Qualifier("dynamicImplSourceDirectory")
    private File dynamicImplSourceDirectory;

    public void refresh() {
        GradleProjectApplicationContext newChildContext = createNewChildContext();
        setNewDelegatesInMainContext(newChildContext);
        closeOldChildContextAndSetNewReference(newChildContext);
    }

    private GradleProjectApplicationContext createNewChildContext() {
        DynamicImplProperties.GitRepository gitRepositoryProperties = dynamicImplProperties.getGitRepository();
        GradleProjectApplicationContext newContext = new GradleProjectApplicationContext(
                mainContext,
                gitRepositoryProperties.getUrl(),
                gitRepositoryProperties.getUsername(),
                gitRepositoryProperties.getPassword(),
                dynamicImplSourceDirectory
        );
        newContext.refresh();
        return newContext;
    }

    private void setNewDelegatesInMainContext(GradleProjectApplicationContext newContext) {

        @SuppressWarnings("rawtypes")
        Map<String, DynamicComponentProxy> proxiesMap = mainContext.getBeansOfType(DynamicComponentProxy.class);

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

    private void closeOldChildContextAndSetNewReference(GradleProjectApplicationContext newChildContext) {

        // Closing the old context
        if (childContext != null) {
            childContext.close();
        }

        // Saving the new context in the field
        childContext = newChildContext;
    }

    @SneakyThrows
    public void init() {
        if (dynamicImplSourceDirectory.exists()) {
            FileUtils.forceDelete(dynamicImplSourceDirectory);
        }
        refresh();
    }

    @EventListener(ContextClosedEvent.class)
    public void close() {
        closeOldChildContextAndSetNewReference(null);
    }
}
