package org.reactivecouchbase.webstrack.env;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.stream.ActorMaterializer;
import com.typesafe.config.ConfigFactory;
import org.reactivecouchbase.webstrack.config.Configuration;
import org.reactivecouchbase.webstrack.utils.FakeExecutorService;

import java.util.concurrent.ExecutorService;

public class Env {

    private static final Configuration DEFAULT = Configuration.of(ConfigFactory.load());

    private static final ActorSystem system = ActorSystem.create("global-system",
            configuration().underlying.atPath("webstack.systems.global").withFallback(ConfigFactory.empty()));
    private static final ActorMaterializer generalPurposeMaterializer = ActorMaterializer.create(system);
    private static final FakeExecutorService globalExecutor = new FakeExecutorService(system.dispatcher());

    private static  final ActorSystem blockingSystem = ActorSystem.create("blocking-system",
            configuration().underlying.atPath("webstack.systems.blocking").withFallback(ConfigFactory.empty()));
    private static final ActorMaterializer blockingActorMaterializer = ActorMaterializer.create(blockingSystem);
    private static final FakeExecutorService blockingExecutor = new FakeExecutorService(blockingSystem.dispatcher());

    private static final ActorSystem wsSystem = ActorSystem.create("ws-system",
            configuration().underlying.atPath("webstack.systems.ws").withFallback(ConfigFactory.empty()));
    private static final ActorMaterializer wsClientActorMaterializer = ActorMaterializer.create(wsSystem);
    private static final FakeExecutorService wsExecutor = new FakeExecutorService(wsSystem.dispatcher());
    private static final Http wsHttp = Http.get(wsSystem);

    private static final ActorSystem websocketSystem = ActorSystem.create("websocket-system",
            configuration().underlying.atPath("webstack.systems.websocket").withFallback(ConfigFactory.empty()));
    private static final ActorMaterializer websocketActorMaterializer = ActorMaterializer.create(websocketSystem);
    private static final FakeExecutorService websocketExecutor = new FakeExecutorService(websocketSystem.dispatcher());
    private static final Http websocketHttp = Http.get(websocketSystem);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            system.terminate();
            blockingSystem.terminate();
            wsSystem.terminate();
            websocketSystem.terminate();
        }));
    }


    public static Configuration configuration() {
        return DEFAULT;
    }

    public static ActorSystem globalActorSystem() {
        return system;
    }

    public static ActorMaterializer generalPurposeMaterializer() {
        return generalPurposeMaterializer;
    }

    public static ExecutorService globalExecutor() {
        return globalExecutor;
    }

    public static ActorSystem blockingSystem() {
        return blockingSystem;
    }

    public static ActorMaterializer blockingActorMaterializer() {
        return blockingActorMaterializer;
    }

    public static ExecutorService blockingExecutor() {
        return blockingExecutor;
    }

    public static ActorSystem wsSystem() {
        return wsSystem;
    }

    public static ActorMaterializer wsClientActorMaterializer() {
        return wsClientActorMaterializer;
    }

    public static ExecutorService wsExecutor() {
        return wsExecutor;
    }

    public static Http wsHttp() {
        return wsHttp;
    }

    public static ActorSystem websocketSystem() {
        return websocketSystem;
    }

    public static ActorMaterializer websocketActorMaterializer() {
        return websocketActorMaterializer;
    }

    public static ExecutorService websocketExecutor() {
        return websocketExecutor;
    }

    public static Http websocketHttp() {
        return websocketHttp;
    }

    public static Mode mode() {
        return Mode.valueOf(configuration().getString("app.mode").getOrElse("Prod"));
    }
}
