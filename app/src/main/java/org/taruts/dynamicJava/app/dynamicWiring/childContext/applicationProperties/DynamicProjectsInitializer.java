package org.taruts.dynamicJava.app.dynamicWiring.childContext.applicationProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProject;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProjectsContainer;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class DynamicProjectsInitializer {

    @Autowired
    private DynamicProjectsApplicationProperties dynamicProjectsApplicationProperties;

    @Autowired
    private DynamicProjectsContainer dynamicProjectsContainer;

    @PostConstruct
    private void init() {
        Map<String, DynamicProject> dynamicProjects = DynamicProjectsApplicationPropertiesMapper.map(
                this.dynamicProjectsApplicationProperties
        );
        dynamicProjectsContainer.setProjects(dynamicProjects);
    }
}
