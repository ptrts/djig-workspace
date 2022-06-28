package org.taruts.dynamicJava.app.dynamicWiring.childContext.configurationProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProject;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProjectManager;

import javax.annotation.PostConstruct;

@Component
public class DynamicProjectLoaderFromConfigurationProperties {

    @Autowired
    private DynamicProjectsConfigurationProperties dynamicProjectsConfigurationProperties;

    @Autowired
    private DynamicProjectManager dynamicProjectManager;

    @PostConstruct
    private void createAndLoadDynamicProjects() {
        this.dynamicProjectsConfigurationProperties.forEach((dynamicProjectName, dynamicProjectConfigurationProperties) -> {
            DynamicProject dynamicProject = DynamicProjectConfigurationPropertiesMapper.map(dynamicProjectName, dynamicProjectConfigurationProperties);
            dynamicProjectManager.addProject(dynamicProject);
        });
    }
}
