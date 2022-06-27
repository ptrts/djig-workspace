package org.taruts.dynamicJava.app.dynamicWiring.childContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProjectsContainer;

import javax.annotation.PostConstruct;

/**
 * Loads the dynamic code on application startup
 */
@Profile({"dev", "prod"})
@Component
public class DynamicProjectContextsInitializer {

    @Autowired
    private DynamicProjectContextManager dynamicProjectContextManager;

    @Autowired
    private DynamicProjectsContainer dynamicProjectsContainer;

    @PostConstruct
    private void init() {
        dynamicProjectsContainer.forEachProject(dynamicProjectContextManager::init);
    }
}
