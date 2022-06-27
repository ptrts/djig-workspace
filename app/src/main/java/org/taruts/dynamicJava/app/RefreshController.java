package org.taruts.dynamicJava.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProjectProperties;
import org.taruts.dynamicJava.app.dynamicWiring.DynamicProjectsProperties;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.GradleProjectApplicationContextContainer;

/**
 * Serves requests to refresh the dynamic code.
 * The "refresh" means to clone, to build and replace the dynamic components by their newer versions.
 * Such requests are expected to come from a GitLab webhook.
 */
@RestController
@RequestMapping("refresh/{dynamicProjectName}")
@Slf4j
public class RefreshController {

    @Autowired
    private GradleProjectApplicationContextContainer gradleProjectApplicationContextContainer;

    @Autowired
    private DynamicProjectsProperties dynamicProjectsProperties;

    @Autowired
    private TaskExecutor taskExecutor;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public void getOrPost(@PathVariable("dynamicProjectName") String dynamicProjectName) {
        refreshAsync(dynamicProjectName);
    }

    private void refreshAsync(String dynamicProjectName) {
        taskExecutor.execute(() -> refresh(dynamicProjectName));
    }

    private void refresh(String dynamicProjectName) {
        DynamicProjectProperties dynamicProjectProperties = dynamicProjectsProperties.get(dynamicProjectName);
        gradleProjectApplicationContextContainer.refresh(dynamicProjectName, dynamicProjectProperties);
    }
}
