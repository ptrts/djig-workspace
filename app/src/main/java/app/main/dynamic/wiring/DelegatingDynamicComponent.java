package app.main.dynamic.wiring;

import app.dynamic.api.DynamicComponent;
import org.springframework.stereotype.Component;

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
