package org.reactivecouchbase.webstrack.websocket;

import org.joda.time.DateTime;
import org.reactivecouchbase.functional.Option;

public class TextWebSocketMessage implements WebSocketMessage {

    private final DateTime dateTime;
    private final String payload;

    public TextWebSocketMessage(DateTime dateTime, String payload) {
        this.dateTime = dateTime;
        this.payload = payload;
    }

    @Override
    public WebSocketMessageType type() {
        return WebSocketMessageType.TEXT;
    }

    @Override
    public DateTime date() {
        return dateTime;
    }

    @Override
    public <T> Option<T> as(Class<T> clazz) {
        if (clazz.equals(TextWebSocketMessage.class)) {
            return Option.some(clazz.cast(this));
        }
        return Option.none();
    }

    public String payload() {
        return payload;
    }
}
