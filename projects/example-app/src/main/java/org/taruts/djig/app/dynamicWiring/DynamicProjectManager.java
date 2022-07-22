package org.taruts.djig.app.dynamicWiring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.taruts.djig.app.dynamicWiring.childContext.DynamicProjectContextManager;

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
