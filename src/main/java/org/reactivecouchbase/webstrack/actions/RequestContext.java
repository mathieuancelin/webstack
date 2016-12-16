package org.reactivecouchbase.webstrack.actions;

import akka.http.scaladsl.coding.Gzip$;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.AsPublisher;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import javaslang.collection.HashMap;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.webstrack.env.Env;
import org.reactivecouchbase.webstrack.config.Configuration;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

public class RequestContext {

    private final HashMap<String, Object> state;

    private final HttpServerExchange httpServerExchange;

    private final ExecutorService ec;

    private final RequestHeaders headers;

    private final RequestQueryParams queryParams;

    private final RequestCookies cookies;

    private final RequestPathParams pathParams;

    private final Configuration configuration;

    public RequestContext(HashMap<String, Object> state, HttpServerExchange httpServerExchange, ExecutorService ec) {
        this.state = state;
        this.httpServerExchange = httpServerExchange;
        this.headers = new RequestHeaders(httpServerExchange);
        this.queryParams = new RequestQueryParams(httpServerExchange);
        this.cookies = new RequestCookies(httpServerExchange);
        this.pathParams = new RequestPathParams(httpServerExchange);
        this.ec = ec;
        this.configuration = Env.configuration();
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

    public RequestContext setValue(String key, Object value) {
        if(key == null || value == null) {
            return this;
        } else {
            return new RequestContext(state.put(key, value), httpServerExchange, ec);
        }
    }

    public String uri() {
        return exchange().getRequestURI();
    }

    public String method() {
        return exchange().getRequestMethod().toString();
    }

    public String chartset() {
        return exchange().getRequestCharset();
    }

    public Long contentLength() {
        return exchange().getRequestContentLength();
    }

    public String path() {
        return exchange().getRequestPath();
    }

    public String scheme() {
        return exchange().getRequestScheme();
    }

    public long startTime() {
        return exchange().getRequestStartTime();
    }

    public String url() {
        return exchange().getRequestURL();
    }

    public String hostAndPort() {
        return exchange().getHostAndPort();
    }

    public String hostName() {
        return exchange().getHostName();
    }

    public int port() {
        return exchange().getHostPort();
    }

    public String protocol() {
        return exchange().getProtocol().toString();
    }

    public String queryString() {
        return exchange().getQueryString();
    }

    public String relativePath() {
        return exchange().getRelativePath();
    }

    public InetSocketAddress sourceAddress() {
        return exchange().getSourceAddress();
    }

    public int status() {
        return exchange().getStatusCode();
    }

    public HttpServerExchange exchange() {
        return httpServerExchange;
    }

    public Future<RequestBody> body() {
        return body(Env.blockingExecutor());
    }

    public Future<RequestBody> body(ExecutorService ec) {
        ActorMaterializer materializer = Env.blockingActorMaterializer();
        return Future.fromJdkCompletableFuture(
            bodyAsStream().runFold(ByteString.empty(), ByteString::concat, materializer).toCompletableFuture()
        ).map(RequestBody::new, ec);
    }

    public <T> Future<T> body(BiFunction<RequestHeaders, Source<ByteString, ?>, Future<T>> bodyParser) {
        return bodyParser.apply(headers, bodyAsStream());
    }

    public <T> Future<T> body(BiFunction<RequestHeaders, Publisher<ByteString>, Future<T>> bodyParser, AsPublisher asPublisher) {
        return bodyParser.apply(headers, bodyAsPublisher(asPublisher));
    }

    public Source<ByteString, ?> bodyAsStream() {
        if (header("Content-Encoding").getOrElse("none").equalsIgnoreCase("gzip")) {
            return rawBodyAsStream().via(Gzip$.MODULE$.decoderFlow());
        }
        return rawBodyAsStream();
    }

    public Source<ByteString, ?> rawBodyAsStream() {
        // TODO : optimize it without blocking
        Publisher<ByteString> publisher = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    // read n and send
                }

                @Override
                public void cancel() {
                    // close and cleanup
                    subscriber.onComplete();
                }
            });
        };

        return StreamConverters.fromInputStream(() -> {
            httpServerExchange.startBlocking();
            return httpServerExchange.getInputStream();
        });
    }

    public Publisher<ByteString> bodyAsPublisher(AsPublisher asPublisher) {
        ActorMaterializer materializer = Env.blockingActorMaterializer();
        return bodyAsStream().runWith(Sink.asPublisher(asPublisher), materializer);
    }

    public Publisher<ByteString> rawBodyAsPublisher(AsPublisher asPublisher) {
        ActorMaterializer materializer = Env.blockingActorMaterializer();
        return rawBodyAsStream().runWith(Sink.asPublisher(asPublisher), materializer);
    }

    public Option<String> header(String name) {
        return Option.apply(exchange().getRequestHeaders().getFirst(name));
    }

    public RequestHeaders headers() {
        return headers;
    }

    public RequestQueryParams queryParams() {
        return queryParams;
    }

    public Option<String> queryParam(String name) {
        return queryParams.param(name);
    }

    public RequestCookies cookies() {
        return cookies;
    }

    public Option<Cookie> cookie(String name) {
        return cookies.cookie(name);
    }

    public RequestPathParams pathParams() {
        return pathParams;
    }

    public Option<String> pathParam(String name) {
        return pathParams.param(name);
    }

    public Configuration configuration() {
        return configuration;
    }
}
