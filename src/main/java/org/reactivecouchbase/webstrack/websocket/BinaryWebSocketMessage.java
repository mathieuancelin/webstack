package org.reactivecouchbase.webstrack.websocket;

import akka.util.ByteString;
import org.joda.time.DateTime;
import org.reactivecouchbase.functional.Option;

public class BinaryWebSocketMessage implements WebSocketMessage {

    private final DateTime dateTime;
    private final ByteString payload;

    public BinaryWebSocketMessage(DateTime dateTime, ByteString payload) {
        this.dateTime = dateTime;
        this.payload = payload;
    }

    @Override
    public WebSocketMessageType type() {
        return WebSocketMessageType.BINARY;
    }

    @Override
    public DateTime date() {
        return dateTime;
    }

    @Override
    public <T> Option<T> as(Class<T> clazz) {
        if (clazz.equals(BinaryWebSocketMessage.class)) {
            return Option.some(clazz.cast(this));
        }
        return Option.none();
    }

    public ByteString payload() {
        return payload;
    }
}
