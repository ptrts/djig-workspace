package org.taruts.dynamicJava.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.taruts.dynamicJava.app.dynamicWiring.childContext.GradleProjectApplicationContextContainer;

/**
 * Serves requests to refresh the dynamic code.
 * The "refresh" means to clone, to build and replace the dynamic components by their newer versions.
 * Such requests are expected to come from a GitLab webhook.
 */
@RestController
@RequestMapping("refresh")
@Slf4j
public class RefreshController {

    @Autowired
    private GradleProjectApplicationContextContainer gradleProjectApplicationContextContainer;

    @GetMapping
    @Async
    public void getRefresh() {
        refresh();
    }

    @PostMapping
    @Async
    public void postRefresh() {
        refresh();
    }

    private void refresh() {
        gradleProjectApplicationContextContainer.refresh();
    }
}
