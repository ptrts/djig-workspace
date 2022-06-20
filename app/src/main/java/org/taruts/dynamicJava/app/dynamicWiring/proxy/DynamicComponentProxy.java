package org.taruts.dynamicJava.app.dynamicWiring.proxy;

import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

/**
 * One of the two implementations of {@link DynamicComponent}.
 * The first one (not this one) sits in the dynamic code.
 * We will call it the dynamic {@link DynamicComponent} implementation.
 * When the dynamic code is refreshed a new version of the dynamic implementation is created and the previous one is disposed of.
 * The dynamic implementation is not a part of the main Spring context, it sits in a special context for dynamic components.
 * <p>
 * This class {@link DynamicComponentProxy} is the other {@link DynamicComponent} implementation.
 * It proxies the first one (the dynamic one).
 * It is created in the main application context at the application startup and is not changed through all the refreshes.
 * As the result of a refresh the new dynamic {@link DynamicComponent} implementation replaces the previous one as the delegate inside
 * {@link DynamicComponentProxy}.
 */
public abstract class DynamicComponentProxy<T extends DynamicComponent> {

    /**
     * The current dynamic implementation
     */
    private T delegate;

    protected T getDelegate() throws DelegateNotSetException {
        if (delegate == null) {
            throw new DelegateNotSetException();
        }
        return delegate;
    }

    public void setDelegate(T delegate) {
        this.delegate = delegate;
    }
}
