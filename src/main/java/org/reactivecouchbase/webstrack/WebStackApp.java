package org.reactivecouchbase.webstrack;

import akka.http.javadsl.model.HttpMethod;
import io.undertow.server.RoutingHandler;
import org.reactivecouchbase.webstrack.mvc.actions.Action;
import org.reactivecouchbase.webstrack.server.ReactiveHttpHandler;

import java.util.function.Supplier;

import static io.undertow.Handlers.routing;

public abstract class WebStackApp {

    RoutingHandler routingHandler = routing();

    public void defineRoutes() {

    }

    public void $(HttpMethod method, String url, Supplier<Action> action) {
        route(method, url, action);
    }

    public void r(HttpMethod method, String url, Supplier<Action> action) {
        route(method, url, action);
    }

    public void route(HttpMethod method, String url, Supplier<Action> action) {
        routingHandler.add(method.name(), url, new ReactiveHttpHandler(action));
    }

    public void beforeStart() {

    }

    public void afterStart() {

    }

    public void beforeStop() {

    }

    public void afterStop() {

    }

    public BootstrappedContext startApp() {
        return WebStack.startWebStackApp(this);
    }
}
