package org.reactivecouchbase.webstrack;

import akka.http.javadsl.model.HttpMethod;
import io.undertow.server.RoutingHandler;
import org.reactivecouchbase.webstrack.actions.ActionSupplier;
import org.reactivecouchbase.webstrack.actions.ReactiveActionHandler;
import org.reactivecouchbase.webstrack.websocket.ReactiveWebSocketHandler;
import org.reactivecouchbase.webstrack.websocket.WebSocketActionSupplier;

import static io.undertow.Handlers.routing;
import static io.undertow.Handlers.websocket;

public abstract class WebStackApp {

    RoutingHandler routingHandler = routing();

    public void defineRoutes() {

    }

    public void $(HttpMethod method, String url, ActionSupplier action) {
        route(method, url, action);
    }

    public void $(HttpMethod method, String url, WebSocketActionSupplier action) {
        route(method, url, action);
    }

    public void r(HttpMethod method, String url, ActionSupplier action) {
        route(method, url, action);
    }

    public void r(HttpMethod method, String url, WebSocketActionSupplier action) {
        route(method, url, action);
    }

    public void route(HttpMethod method, String url, ActionSupplier action) {
        routingHandler.add(method.name(), url, new ReactiveActionHandler(action));
    }

    public void route(HttpMethod method, String url, WebSocketActionSupplier action) {
        routingHandler.add(method.name(), url, websocket(new ReactiveWebSocketHandler(action)));
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
