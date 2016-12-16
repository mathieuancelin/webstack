package org.reactivecouchbase.webstrack.websocket;

import org.joda.time.DateTime;
import org.reactivecouchbase.functional.Option;

public interface WebSocketMessage {

    WebSocketMessageType type();

    DateTime date();

    <T> Option<T> as(Class<T> clazz);
}
