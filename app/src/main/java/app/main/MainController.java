package app.main;

import app.dynamic.api.DynamicComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MainController {

    @Autowired
    private DynamicComponent dynamicComponent;

    @GetMapping("message")
    public String get() {
        return dynamicComponent.getMessage();
    }
}
