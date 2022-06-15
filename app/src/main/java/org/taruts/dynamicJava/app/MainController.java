package org.taruts.dynamicJava.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.taruts.dynamicJava.app.dynamicWiring.DelegatingDynamicComponent;
import org.taruts.dynamicJava.dynamicApi.DynamicComponent;

@RestController
@Slf4j
public class MainController {

    @Autowired
    private DynamicComponent dynamicComponent;

    @GetMapping("message")
    public String get() {
        try {
            return dynamicComponent.getMessage();
        } catch (DelegatingDynamicComponent.DelegateNotSetException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
