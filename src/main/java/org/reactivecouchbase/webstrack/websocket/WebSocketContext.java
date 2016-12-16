package org.reactivecouchbase.webstrack.websocket;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import javaslang.collection.HashMap;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.webstrack.config.Configuration;

import java.util.concurrent.ExecutorService;

public class WebSocketContext {

    private final WebSocketHttpExchange exchange;
    private final WebSocketChannel channel;
    private final WebSocketRequestHeaders headers;
    private final WebSocketRequestQueryParams queryParams;
    private final WebSocketRequestPathParams pathParams;
    private final ExecutorService ec;
    private final Configuration configuration;
    private final HashMap<String, Object> state;

    public WebSocketContext(HashMap<String, Object> state, WebSocketHttpExchange exchange, WebSocketChannel channel, Configuration configuration, ExecutorService ec) {
        this.ec = ec;
        this.state = state;
        this.exchange = exchange;
        this.channel = channel;
        this.configuration = configuration;
        this.headers = new WebSocketRequestHeaders(exchange);
        this.queryParams = new WebSocketRequestQueryParams(exchange);
        this.pathParams = new WebSocketRequestPathParams(exchange);
    }

    public WebSocketHttpExchange exchange() {
        return exchange;
    }

    public ExecutorService currentExecutor() {
        return ec;
    }

    public Object getValue(String key) {
        return this.state.get(key);
    }

    public <T> T getValue(String key, Class<T> clazz) {
        return this.state.get(key).map(clazz::cast).get();
    }

    public WebSocketContext setValue(String key, Object value) {
        if(key == null || value == null) {
            return this;
        } else {
            return new WebSocketContext(state.put(key, value), exchange, channel, configuration, ec);
        }
    }

    public Option<String> header(String name) {
        return headers.header(name);
    }

    public WebSocketRequestHeaders headers() {
        return headers;
    }

    public WebSocketRequestQueryParams queryParams() {
        return queryParams;
    }

    public Option<String> queryParam(String name) {
        return queryParams.param(name);
    }

    public WebSocketRequestPathParams pathParams() {
        return pathParams;
    }

    public Option<String> pathParam(String name) {
        return pathParams.param(name);
    }

    public Configuration configuration() {
        return configuration;
    }
}