package org.reactivecouchbase.webstrack.server;

import akka.Done;
import akka.japi.Pair;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.json.mapping.ThrowableWriter;
import org.reactivecouchbase.webstrack.env.Env;
import org.reactivecouchbase.webstrack.mvc.actions.Action;
import org.reactivecouchbase.webstrack.mvc.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class ReactiveHttpHandler implements HttpHandler {

    static final Logger logger = LoggerFactory.getLogger(ReactiveHttpHandler.class);

    private final Supplier<Action> action;

    public ReactiveHttpHandler(Supplier<Action> action) {
        this.action = action;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // UndertowOptions
        exchange.setMaxEntitySize(Long.MAX_VALUE);
        exchange.dispatch(() -> {
            action.get().run(exchange).andThen(resultTry -> {
                for (Result result : resultTry.asSuccess()) {
                    result.headers.forEach(tuple -> exchange.getResponseHeaders().putAll(HttpString.tryFromString(tuple._1), tuple._2.toJavaList()));
                    result.cookies.forEach(cookie -> exchange.getResponseCookies().put(cookie.getName(), cookie));
                    exchange.setStatusCode(result.status);
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, result.contentType);
                    exchange.getResponseHeaders().put(HttpString.tryFromString("Transfer-Encoding"), "chunked");
                    exchange.getResponseHeaders().put(HttpString.tryFromString("X-Transfer-Encoding"), "chunked");
                    // exchange.getResponseHeaders().put(HttpString.tryFromString("X-Content-Type"), result.contentType);
                    StreamSinkChannel responseChannel = exchange.getResponseChannel();
                    Pair<?, CompletionStage<Done>> run = result.source.toMat(Sink.foreach(bs -> {
                        // logger.trace("chunk: " + bs.utf8String());
                        responseChannel.write(bs.asByteBuffer());
                    }), Keep.both()).run(Env.blockingActorMaterializer());
                    result.materializedValue.trySuccess(run.first());
                    run.second().whenComplete((success, error) -> {
                        try {
                            responseChannel.flush();
                            exchange.endExchange();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                for (Throwable t : resultTry.asFailure()) {
                    exchange.setStatusCode(500);
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    exchange.getResponseSender().send(
                        Json.obj().with("error",
                            new ThrowableWriter(true).write(t))
                                .stringify()
                    );
                    exchange.endExchange();
                }
            }, Env.blockingExecutor());
        });

    }
}

/*

Undertow server = Undertow.builder()
    .addHttpListener(8080, "localhost")
    .setHandler(path()
        .addPrefixPath("/sse", sseHandler)
        .addPrefixPath("/send", chatHandler)
        .addPrefixPath("/", resource(new ClassPathResourceManager(ServerSentEventsServer.class.getClassLoader(), ServerSentEventsServer.class.getPackage())).addWelcomeFiles("index.html")))
    .build();
server.start();
 */
