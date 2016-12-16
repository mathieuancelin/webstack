package org.reactivecouchbase.webstrack;

import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.util.HttpString;
import javaslang.collection.List;
import org.reactivecouchbase.common.Throwables;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.webstrack.env.Env;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;

public class WebStack {

    static final Logger logger = LoggerFactory.getLogger(WebStack.class);

    public static void main(String... args) {
        logger.trace("Scanning classpath looking for WebStackApp implementations");
        Reflections reflections = new Reflections("");
        List.ofAll(reflections.getSubTypesOf(WebStackApp.class))
            .headOption()
            .map(serverClazz -> {
                try {
                    logger.info("Found WebStackApp class: " + serverClazz.getName());
                    WebStackApp context = serverClazz.newInstance();
                    return startWebStackApp(context);
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            });
    }

    static BootstrappedContext startWebStackApp(WebStackApp webstackApp) {
        logger.trace("Starting WebStackApp");
        int port = Env.configuration().getInt("webstack.port").getOrElse(9000);
        String host = Env.configuration().getString("webstack.host").getOrElse("0.0.0.0");
        webstackApp.defineRoutes();
        RoutingHandler handler = webstackApp.routingHandler.setInvalidMethodHandler(ex -> {
            // TODO : do it better
            ex.setStatusCode(400);
            ex.getResponseHeaders().put(HttpString.tryFromString("Content-Type"), "application/json");
            ex.getResponseSender().send(Json
                    .obj()
                    .with("error", "Invalid Method " + ex.getRequestMethod() + " on uri " + ex.getRequestURI())
                    .stringify());
        }).setFallbackHandler(path().addPrefixPath("/assets",
            resource(new ClassPathResourceManager(WebStack.class.getClassLoader(), "public")))
        );
        logger.trace("Starting Undertow");
        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(handler)
                .build();
        webstackApp.beforeStart();
        server.start();
        webstackApp.afterStart();
        logger.trace("Undertow started");
        logger.info("Running WebStack on http://" + host + ":" + port);
        BootstrappedContext bootstrapedContext = new BootstrappedContext(server, webstackApp);
        logger.trace("Registering shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(bootstrapedContext::stopApp));
        logger.trace("Init done");
        return bootstrapedContext;
    }
}
