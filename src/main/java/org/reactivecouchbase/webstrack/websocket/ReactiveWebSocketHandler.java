package org.reactivecouchbase.webstrack.websocket;

import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueueWithComplete;
import akka.util.ByteString;
import io.undertow.server.protocol.framed.AbstractFramedChannel;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.*;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import org.joda.time.DateTime;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.webstrack.env.Env;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ReactiveWebSocketHandler implements WebSocketConnectionCallback {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveWebSocketHandler.class);

    private final ConcurrentHashMap<String, SourceQueueWithComplete<WebSocketMessage>> connections = new ConcurrentHashMap<>();

    final Function<WebSocketContext, Future<Flow<WebSocketMessage, WebSocketMessage, ?>>> handler;

    public ReactiveWebSocketHandler(WebSocketActionSupplier supplier) {
        this.handler = supplier.get().handler;
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {

        String id = UUID.randomUUID().toString();

        try {
            Source<WebSocketMessage, SourceQueueWithComplete<WebSocketMessage>> queue = Source.queue(50, OverflowStrategy.backpressure());
            Future<Flow<WebSocketMessage, WebSocketMessage, ?>> flow = handler.apply(new WebSocketContext(
                HashMap.empty(),
                exchange,
                channel,
                Env.configuration(),
                Env.websocketExecutor()
            ));
            flow.onSuccess(f -> {
                SourceQueueWithComplete<WebSocketMessage> matQueue = queue
                        .via(f)
                        .to(Sink.foreach(message -> {
                            for (TextWebSocketMessage m : message.as(TextWebSocketMessage.class)) {
                                WebSockets.sendText(m.payload(), channel, null);
                            }
                            for (BinaryWebSocketMessage m : message.as(BinaryWebSocketMessage.class)) {
                                WebSockets.sendBinary(m.payload().asByteBuffer(), channel, null);
                            }
                        }))
                        .run(Env.websocketActorMaterializer());
                matQueue.watchCompletion().thenAccept(done -> {
                    try {
                        exchange.endExchange();
                    } catch (Exception e) {
                        logger.error("Error while closing websocket session", e);
                    }
                });
                connections.put(id, matQueue);
            });
        } catch (Exception e) {
            logger.error("Error after Websocket connection established", e);
        }

        ChannelListener<WebSocketChannel> listener = new AbstractReceiveListener() {

            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                try {
                    get(id).forEach(queue -> queue.offer(new TextWebSocketMessage(DateTime.now(), message.getData())));
                } catch (Exception e) {
                    logger.error("Error while handling Websocket message", e);
                }
            }

            @Override
            protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                try {
                    ByteString bs = List.ofAll(Arrays.asList(message.getData().getResource()))
                            .map(ByteString::fromByteBuffer)
                            .foldLeft(ByteString.empty(), ByteString::concat);
                    get(id).forEach(queue -> queue.offer(new BinaryWebSocketMessage(DateTime.now(), bs)));
                } catch (Exception e) {
                    logger.error("Error while handling Websocket message", e);
                }
            }

            @Override
            protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) throws IOException {
                try {
                    get(id).forEach(SourceQueueWithComplete::complete);
                    connections.remove(id);
                } catch (Exception e) {
                    logger.error("Error after closing Websocket connection", e);
                }
            }
        };

        channel.getReceiveSetter().set(listener);
        channel.getCloseSetter().set(new ChannelListener<AbstractFramedChannel>() {
            @Override
            public void handleEvent(AbstractFramedChannel channel) {
                try {
                    get(id).forEach(SourceQueueWithComplete::complete);
                    connections.remove(id);
                } catch (Exception e) {
                    logger.error("Error after closing Websocket connection", e);
                }
            }
        });
        channel.resumeReceives();
    }

    private Option<SourceQueueWithComplete<WebSocketMessage>> get(String id) {
        return Option.apply(connections.get(id));
    }
}
