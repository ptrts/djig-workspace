package org.taruts.djig.app;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

@Slf4j
public abstract class OurSmartLifecycle implements SmartLifecycle {

    private boolean running = false;

    @Override
    @SneakyThrows
    public final void start() {
        doStart();
        running = true;
    }

    @Override
    public final void stop() {
        doStop();
        running = false;
    }

    protected abstract void doStop();

    @Override
    public boolean isRunning() {
        return running;
    }

    protected abstract void doStart();
}
