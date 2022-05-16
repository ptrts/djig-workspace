package app.main.dynamic.wiring;

import app.dynamic.api.DynamicComponent;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

@Component
public class GradleProjectApplicationContextContainer {

    private GradleProjectApplicationContext gradleProjectApplicationContext;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DelegatingDynamicComponent delegatingDynamicComponent;

    @Autowired
    private DynamicImplProperties dynamicImplProperties;

    @Autowired
    @Qualifier("dynamicImplSourceDirectory")
    private File dynamicImplSourceDirectory;

    public void refresh() {

        delegatingDynamicComponent.setDelegate(null);

        close();

        DynamicImplProperties.GitRepository gitRepositoryProperties = dynamicImplProperties.getGitRepository();
        gradleProjectApplicationContext = new GradleProjectApplicationContext(
                applicationContext,
                gitRepositoryProperties.getUrl(),
                gitRepositoryProperties.getUsername(),
                gitRepositoryProperties.getPassword(),
                dynamicImplSourceDirectory
        );
        gradleProjectApplicationContext.refresh();

        Map<String, DynamicComponent> dynamicComponentsMap = gradleProjectApplicationContext.getBeansOfType(DynamicComponent.class);
        List<DynamicComponent> dynamicComponents = dynamicComponentsMap
                .values()
                .stream()
                .filter(currentDynamicComponent -> currentDynamicComponent != delegatingDynamicComponent)
                .toList();
        if (dynamicComponents.size() != 1) {
            throw new IllegalStateException(
                    "dynamicComponents.size() = %d".formatted(
                            dynamicComponents.size()
                    )
            );
        }
        DynamicComponent childContextDynamicComponent = dynamicComponents.get(0);
        delegatingDynamicComponent.setDelegate(childContextDynamicComponent);
    }

    @SneakyThrows
    public void init() {
        FileUtils.forceDelete(dynamicImplSourceDirectory);
        refresh();
    }

    @EventListener(ContextClosedEvent.class)
    public void close() {
        if (gradleProjectApplicationContext != null) {
            gradleProjectApplicationContext.close();
            gradleProjectApplicationContext = null;
        }
    }
}
