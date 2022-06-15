package org.taruts.dynamicJava.app.dynamicWiring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
