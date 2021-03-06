package org.reactivecouchbase.webstrack.websocket;

import io.undertow.websockets.spi.WebSocketHttpExchange;
import javaslang.collection.*;
import org.reactivecouchbase.functional.Option;

public class WebSocketRequestHeaders {

    private final Map<String, List<String>> headers;

    WebSocketRequestHeaders(WebSocketHttpExchange request) {
        this.headers = Option.apply(request.getRequestHeaders()).map(headers -> {
            Map<String, List<String>> _headers = HashMap.empty();
            for (String name : headers.keySet()) {
                _headers = _headers.put(name, List.ofAll(headers.get(name)));
            }
            return _headers;
        }).getOrElse(HashMap.empty());
    }

    public Option<String> header(String name) {
        return headers.get(name).flatMap(Traversable::headOption).transform(opt -> {
            if (opt.isDefined()) {
                return Option.apply(opt.get());
            } else {
                return Option.none();
            }
        });
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public Map<String, String> simpleHeaders() {
        return headers.bimap(k -> k, Traversable::head);
    }

    public Set<String> headerNames() {
        return headers.keySet();
    }

    public Map<String, List<String>> raw() {
        return headers;
    }
}
