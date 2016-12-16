package org.reactivecouchbase.webstrack.websocket;

import akka.stream.javadsl.Flow;
import org.reactivecouchbase.concurrent.Future;

import java.util.function.Function;

public class WebSocketAction {

    public final Function<WebSocketContext, Future<Flow<WebSocketMessage, WebSocketMessage, ?>>> handler;

    private WebSocketAction(Function<WebSocketContext, Future<Flow<WebSocketMessage, WebSocketMessage, ?>>> handler) {
        this.handler = handler;
    }

    public static WebSocketAction accept(Function<WebSocketContext, Flow<WebSocketMessage, WebSocketMessage, ?>> handler) {
        return new WebSocketAction(ctx -> Future.successful(handler.apply(ctx)));
    }

    public static WebSocketAction acceptAsync(Function<WebSocketContext, Future<Flow<WebSocketMessage, WebSocketMessage, ?>>>  handler) {
        return new WebSocketAction(handler);
    }
}