package org.taruts.dynamicJava.app;

import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.dynamicApi.MainComponent;

@Component
public class MainComponentImpl implements MainComponent {

    @Override
    public String getMessagePrefix() {
        return "Message ";
    }
}
