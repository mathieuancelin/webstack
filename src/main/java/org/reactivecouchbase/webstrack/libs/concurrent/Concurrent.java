package org.reactivecouchbase.webstrack.libs.concurrent;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import com.typesafe.config.ConfigFactory;
import org.reactivecouchbase.webstrack.config.Configuration;
import org.reactivecouchbase.webstrack.utils.FakeExecutorService;

public class Concurrent {

    public static final ActorSystem system = ActorSystem.create("global-system",
            Configuration.current().underlying.atPath("webstack.systems.global").withFallback(ConfigFactory.empty()));
    public static  final ActorMaterializer generalPurposeMaterializer = ActorMaterializer.create(system);
    public static  final FakeExecutorService globalExecutor = new FakeExecutorService(system.dispatcher());

    public static  final ActorSystem blockingSystem = ActorSystem.create("blocking-system",
            Configuration.current().underlying.atPath("webstack.systems.blocking").withFallback(ConfigFactory.empty()));
    public static  final ActorMaterializer blockingActorMaterializer = ActorMaterializer.create(blockingSystem);
    public static  final FakeExecutorService blockingExecutor = new FakeExecutorService(blockingSystem.dispatcher());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            system.terminate();
            blockingSystem.terminate();
        }));
    }

}
