package org.taruts.dynamicJava.app.dynamicWiring.runtime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProject;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProjectRepository;

@Component
public class DynamicImplementationSelector {

    @Autowired
    private DynamicProjectRepository dynamicProjectRepository;

    public <T> T select(Class<T> iface, String dynamicProjectName) {
        DynamicProject dynamicProject = dynamicProjectRepository.getProject(dynamicProjectName);
        return dynamicProject.getContext().getBean(iface);
    }
}
