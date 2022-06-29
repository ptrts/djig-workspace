package org.taruts.djig.app.dynamicWiring.mainContext.proxy;

import org.taruts.dynamicJava.dynamicApi.dynamic.DynamicComponent;

/**
 * An abstract class which every <b>dynamic component proxy</b> must extend.
 * <p>
 * A <b>dynamic component proxy</b> is a bean in the main Spring context
 * proxying a dynamic interface implementation in the dynamic child Spring context.
 * <p>
 * Each dynamic interface has a proxy bean in the main Spring context.
 * <p>
 * Aside from subclassing this class, a dynamic component proxy must implement the dynamic interface
 * implementations of which it is supposed to proxy.
 * <p>
 * Once a new version of the dynamic code arrives, a new version of the dynamic context is created.
 * When this happens all dynamic component proxies in the main context stay in place, but their delegates are changed to their
 * respective dynamic interface implementations from the new dynamic context.
 * <p>
 * First, {@link DynamicComponentProxy} is a marker superclass, this is how we know if a class is a dynamic component proxy.
 * <p>
 * Second, it contains a {@link #delegate} property, which every proxy like that would need.
 */
public abstract class DynamicComponentProxy<DynamicInterface extends DynamicComponent> {

    /**
     * The current implementation of the dynamic interface
     */
    private DynamicInterface delegate;

    /**
     * The only means for a subclass to access the delegate.
     * If the delegate is not set, then an attempt to access it would throw a {@link DelegateNotSetException}
     *
     * @return the delegate if it is set
     * @throws DelegateNotSetException if the delegate is not set
     */
    protected DynamicInterface getDelegate() throws DelegateNotSetException {
        if (delegate == null) {
            throw new DelegateNotSetException();
        }
        return delegate;
    }

    public void setDelegate(DynamicInterface delegate) {
        this.delegate = delegate;
    }
}
