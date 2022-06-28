package org.taruts.dynamicJava.app.dynamicWiring;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class DynamicProjectRepository {

    private final Map<String, DynamicProject> dynamicProjectsMap = Collections.synchronizedMap(new HashMap<>());

    public DynamicProject getProject(String name) {
        return dynamicProjectsMap.get(name);
    }

    public void forEachProject(Consumer<DynamicProject> useDynamicProject) {
        dynamicProjectsMap.values().forEach(useDynamicProject);
    }

    public void addProject(DynamicProject dynamicProject) {
        String dynamicProjectName = dynamicProject.getName();
        if (dynamicProjectsMap.containsKey(dynamicProjectName)) {
            throw new IllegalArgumentException(String.format("A project with the name [%s] already exists", dynamicProjectName));
        }
        dynamicProjectsMap.put(dynamicProjectName, dynamicProject);
    }
}
