package org.taruts.dynamicJava.app.dynamicWiring;

import org.springframework.stereotype.Component;
import org.taruts.dynamicJava.dynamicApi.DynamicComponent;

@Component
public class DelegatingDynamicComponent implements DynamicComponent {

    private DynamicComponent delegate;

    @Override
    public String getMessage() throws DelegateNotSetException {
        if (delegate == null) {
            throw new DelegateNotSetException();
        }
        return delegate.getMessage();
    }

    public void setDelegate(DynamicComponent delegate) {
        this.delegate = delegate;
    }

    public static class DelegateNotSetException extends RuntimeException {
    }
}
