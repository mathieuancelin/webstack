package org.reactivecouchbase.webstack;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.http.javadsl.model.ws.Message;
import org.reactivecouchbase.webstrack.websocket.WebSocketContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketPing extends UntypedActor {

    private final ActorRef out;
    private final WebSocketContext ctx;
    private static final Logger logger = LoggerFactory.getLogger(WebsocketPing.class);

    public WebsocketPing(WebSocketContext ctx, ActorRef out) {
        this.out = out;
        this.ctx = ctx;
    }

    public static Props props(WebSocketContext ctx, ActorRef out) {
        return Props.create(WebsocketPing.class, () -> new WebsocketPing(ctx, out));
    }

    public void onReceive(Object message) throws Exception {
        logger.info("[WebsocketPing] received message {}", message);
        if (message instanceof Message) {
            logger.info("[WebsocketPing] Sending back message");
            out.tell(message, getSelf());
        } else {
            unhandled(message);
        }
    }
}