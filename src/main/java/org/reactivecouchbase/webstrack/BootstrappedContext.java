package org.reactivecouchbase.webstrack;

import io.undertow.Undertow;

public class BootstrappedContext {

    private final Undertow undertow;
    private final WebStackApp app;

    BootstrappedContext(Undertow undertow, WebStackApp app) {
        this.undertow = undertow;
        this.app = app;
    }

    public void stopApp() {
        try {
            app.beforeStop();
            undertow.stop();
            app.afterStop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
