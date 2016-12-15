package org.reactivecouchbase.webstrack.mvc.actions;

import io.undertow.server.HttpServerExchange;
import javaslang.collection.HashMap;
import javaslang.collection.Map;
import javaslang.collection.Set;
import org.reactivecouchbase.functional.Option;

public class RequestPathParams {

    private final Map<String, String> pathParams;

    RequestPathParams(HttpServerExchange request) {
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