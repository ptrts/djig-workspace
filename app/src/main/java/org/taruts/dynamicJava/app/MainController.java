package org.taruts.dynamicJava.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.taruts.dynamicJava.app.dynamicWiring.proxy.DelegateNotSetException;
import org.taruts.dynamicJava.dynamicApi.dynamic.MessageProvider;

@RestController
@Slf4j
public class MainController {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private MessageProvider messageProvider;

    /**
     * The main functionality of the app. It just returns sort of a hello message.
     * The point is that the message is provided by the dynamic java code.
     */
    @GetMapping("message")
    public String get() {
        try {
            // Get the message from the dynamic Java code
            return messageProvider.getMessage();
        } catch (DelegateNotSetException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
