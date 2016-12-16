package org.reactivecouchbase.webstrack.actions;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import javaslang.collection.HashMap;
import javaslang.collection.Map;
import javaslang.collection.Set;
import org.reactivecouchbase.functional.Option;

public class RequestCookies {

    private final Map<String, Cookie> cookies;

    RequestCookies(HttpServerExchange request) {
        this.cookies = Option.apply(request.getRequestCookies()).map(cookies -> {
            Map<String, Cookie> _cookies = HashMap.empty();
            for (java.util.Map.Entry<String, Cookie> cookie : cookies.entrySet()) {
                _cookies = _cookies.put(cookie.getKey(), cookie.getValue());
            }
            return _cookies;
        }).getOrElse(HashMap.empty());
    }

    public Map<String, Cookie> raw() {
        return cookies;
    }

    public Set<String> cookieNames() {
        return cookies.keySet();
    }

    public Option<Cookie> cookie(String name) {
        return cookies.get(name).transform(opt -> {
            if (opt.isDefined()) {
                return Option.apply(opt.get());
            } else {
                return Option.none();
            }
        });
    }
}
