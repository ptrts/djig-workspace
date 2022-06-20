package org.taruts.dynamicJava.app.dynamicWiring.proxy.old;

import org.taruts.dynamicJava.app.dynamicWiring.proxy.DelegateNotSetException;
import org.taruts.dynamicJava.app.dynamicWiring.proxy.DynamicComponentProxy;
import org.taruts.dynamicJava.dynamicApi.dynamic.MessageProvider;

// todo Перенести комменты на новые классы

/**
 * One of the two implementations of {@link MessageProvider}.
 * The first one (not this one) sits in the dynamic code.
 * We will call it the dynamic {@link MessageProvider} implementation.
 * When the dynamic code is refreshed a new version of the dynamic implementation is created and the previous one is disposed of.
 * The dynamic implementation is not a part of the main Spring context, it sits in a special context for dynamic components.
 * <p>
 * This class {@link DelegatingDynamicComponent} is the other {@link MessageProvider} implementation.
 * It proxies the first one (the dynamic one).
 * It is created in the main application context at the application startup and is not changed through all the refreshes.
 * As the result of a refresh the new dynamic {@link MessageProvider} implementation replaces the previous one as the delegate inside
 * {@link DelegatingDynamicComponent}.
 */
public class DelegatingDynamicComponent extends DynamicComponentProxy<MessageProvider> implements MessageProvider {

    /**
     * @throws DelegateNotSetException if the delegate is not set. This should not happen in the process of a refresh, because
     *                                 during a refresh the old delegate stays in place until replaced by the new one. In theory this might happen during
     *                                 a context refresh, if the method is called
     */
    @Override
    public String getMessage() throws DelegateNotSetException {
        return getDelegate().getMessage();
    }
}
