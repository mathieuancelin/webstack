package org.reactivecouchbase.webstrack.mvc.actions;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import javaslang.collection.*;
import org.reactivecouchbase.functional.Option;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

public class RequestHeaders {

    private final Map<String, List<String>> headers;

    RequestHeaders(HttpServerExchange request) {
        this.headers = Option.apply(request.getRequestHeaders()).map(headers -> {
            Map<String, List<String>> _headers = HashMap.empty();
            for (HttpString name : headers.getHeaderNames()) {
                _headers = _headers.put(name.toString(), List.ofAll(headers.get(name)));
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
