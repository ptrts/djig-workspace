package org.taruts.dynamicJava.app.dynamicWiring.childContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Loads the dynamic code on application startup
 */
@Profile({"dev", "prod"})
@Component
public class DynamicProjectInitializer {

    @Autowired
    private GradleProjectApplicationContextContainer gradleProjectApplicationContextContainer;

    @PostConstruct
    private void init() {
        gradleProjectApplicationContextContainer.init();
    }
}
