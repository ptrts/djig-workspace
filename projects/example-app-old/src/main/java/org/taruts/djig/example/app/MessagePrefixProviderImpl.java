package org.taruts.djig.example.app;

import org.springframework.stereotype.Component;
import org.taruts.djig.example.dynamicApi.main.MessagePrefixProvider;

/**
 * The implementation of an interface from dynamic-api.
 *
 * @see MessagePrefixProvider
 */
@Component
public class MessagePrefixProviderImpl implements MessagePrefixProvider {

    @Override
    public String getMessagePrefix() {
        return "Message:";
    }
}
