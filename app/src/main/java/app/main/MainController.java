package app.main;

import app.main.dynamic.wiring.DelegatingDynamicComponent;
import app.main.dynamic.wiring.GradleProjectApplicationContextContainer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MainController {

    @Autowired
    private DelegatingDynamicComponent foo;

    @Autowired
    private GradleProjectApplicationContextContainer gradleProjectApplicationContextContainer;

    @GetMapping("message")
    public String get() {
        return foo.getMessage();
    }

    @GetMapping("refresh")
    @SneakyThrows
    public void refresh() {
        gradleProjectApplicationContextContainer.refresh();
    }
}
