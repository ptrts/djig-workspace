package app.main;

import app.dynamic.api.MainComponent;
import org.springframework.stereotype.Component;

@Component
public class MainComponentImpl implements MainComponent {

    @Override
    public String getMessagePrefix() {
        return "Message ";
    }
}
