package app.main;

import app.main.dynamic.wiring.GradleProjectApplicationContextContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("refresh")
@Slf4j
public class RefreshController {

    @Autowired
    private GradleProjectApplicationContextContainer gradleProjectApplicationContextContainer;

    @GetMapping
    public void getRefresh() {
        refresh();
    }

    @PostMapping
    public void postRefresh() {
        refresh();
    }

    private void refresh() {
        gradleProjectApplicationContextContainer.refresh();
    }
}
