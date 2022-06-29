package org.taruts.djig.app;

import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.dynamicApi.main.MessagePrefixProvider;

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
