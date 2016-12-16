package org.reactivecouchbase.webstrack.websocket;

import io.undertow.websockets.spi.WebSocketHttpExchange;
import javaslang.collection.HashMap;
import javaslang.collection.Map;
import javaslang.collection.Set;
import org.reactivecouchbase.functional.Option;

public class WebSocketRequestPathParams {

    private final Map<String, String> pathParams;

    WebSocketRequestPathParams(WebSocketHttpExchange request) {
        this.pathParams = Option.apply(request.getAttachment(io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY))
                .map(m -> HashMap.ofAll(m.getParameters())).getOrElse(HashMap.empty());
    }

    public Map<String, String> raw() {
        return pathParams;
    }

    public Set<String> paramNames() {
        return pathParams.keySet();
    }

    public Option<String> param(String name) {
        return pathParams.get(name).transform(opt -> {
            if (opt.isDefined()) {
                return Option.apply(opt.get());
            } else {
                return Option.none();
            }
        });
    }
}
