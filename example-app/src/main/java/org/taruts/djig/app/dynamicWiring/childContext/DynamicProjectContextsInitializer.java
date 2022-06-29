package org.taruts.djig.app.dynamicWiring.childContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.taruts.djig.app.dynamicWiring.DynamicProjectRepository;

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
    private DynamicProjectRepository dynamicProjectRepository;

    @PostConstruct
    private void init() {
        dynamicProjectRepository.forEachProject(dynamicProjectContextManager::init);
    }
}
