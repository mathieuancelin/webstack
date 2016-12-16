package org.reactivecouchbase.webstrack.websocket;

import akka.http.javadsl.model.ws.Message;
import akka.stream.javadsl.Flow;
import org.reactivecouchbase.concurrent.Future;

import java.util.function.Function;

public class WebSocketAction {

    public final Function<WebSocketContext, Future<Flow<Message, Message, ?>>> handler;

    private WebSocketAction(Function<WebSocketContext, Future<Flow<Message, Message, ?>>> handler) {
        this.handler = handler;
    }

    public static WebSocketAction accept(Function<WebSocketContext, Flow<Message, Message, ?>> handler) {
        return new WebSocketAction(ctx -> Future.successful(handler.apply(ctx)));
    }

    public static WebSocketAction acceptAsync(Function<WebSocketContext, Future<Flow<Message, Message, ?>>>  handler) {
        return new WebSocketAction(handler);
    }
}