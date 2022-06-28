package org.taruts.dynamicJava.app.dynamicWiring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.DynamicProjectContextManager;

@Component
public class DynamicProjectManager {

    @Autowired
    private DynamicProjectRepository dynamicProjectRepository;

    @Autowired
    private DynamicProjectContextManager dynamicProjectContextManager;

    public void addProject(DynamicProject dynamicProject) {
        dynamicProjectRepository.addProject(dynamicProject);
        dynamicProjectContextManager.init(dynamicProject);
    }
}
