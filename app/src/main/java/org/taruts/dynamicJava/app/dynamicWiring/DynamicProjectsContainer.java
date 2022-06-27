package org.taruts.dynamicJava.app.dynamicWiring;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class DynamicProjectsContainer {

    @Getter
    private Map<String, DynamicProject> dynamicProjectsMap = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    private ApplicationContext applicationContext;

    public DynamicProject getProject(String name) {
        return dynamicProjectsMap.get(name);
    }

    public void setProjects(Map<String, DynamicProject> dynamicProjectsMap) {
        HashMap<String, DynamicProject> newMap = new HashMap<>(dynamicProjectsMap);
        this.dynamicProjectsMap = Collections.synchronizedMap(newMap);
    }

    @EventListener(ContextClosedEvent.class)
    public void close(ContextClosedEvent event) {
        if (event.getApplicationContext() != applicationContext) {
            return;
        }

        forEachProject(dynamicProject -> {
            dynamicProject.getContext().close();
            dynamicProject.setContext(null);
        });
    }

    public void forEachProject(Consumer<DynamicProject> useDynamicProject) {
        dynamicProjectsMap.values().forEach(useDynamicProject);
    }
}
