package org.taruts.dynamicJava.app.dynamicWiring.childContext.context;

import org.springframework.core.env.PropertySource;

/**
 * This allows an Environment to reuse a PropertySource from another Environment (e.g. from one of the parent context),
 * but with a different name, to avoid possible name conflicts.
 */
public class DelegatingPropertySource extends PropertySource<PropertySource<?>> {

    public DelegatingPropertySource(String name, PropertySource source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return source.getProperty(name);
    }
}
