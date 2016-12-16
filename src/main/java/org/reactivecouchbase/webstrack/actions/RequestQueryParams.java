package org.reactivecouchbase.webstrack.actions;

import io.undertow.server.HttpServerExchange;
import javaslang.collection.*;
import org.reactivecouchbase.functional.Option;

public class RequestQueryParams {

    private final Map<String, List<String>> queryParams;

    RequestQueryParams(HttpServerExchange request) {
        this.queryParams = HashMap.ofAll(request.getQueryParameters()).mapValues(List::ofAll);
    }

    public Map<String, List<String>> raw() {
        return queryParams;
    }

    public Map<String, String> simpleParams() {
        return queryParams.bimap(k -> k, Traversable::head);
    }

    public Set<String> paramsNames() {
        return queryParams.keySet();
    }

    public List<String> params(String name) {
        return queryParams.get(name).getOrElse(List.empty());
    }

    public Option<String> param(String name) {
        return queryParams.get(name).flatMap(Traversable::headOption).transform(opt -> {
           if (opt.isDefined()) {
               return Option.apply(opt.get());
           } else {
               return Option.none();
           }
        });
    }
}
