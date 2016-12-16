package org.reactivecouchbase.webstrack.env;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.stream.ActorMaterializer;
import org.reactivecouchbase.webstrack.config.Configuration;

import java.util.concurrent.ExecutorService;

public interface EnvLike {
    Configuration configuration();
    ActorSystem globalActorSystem();
    ActorMaterializer generalPurposeMaterializer();
    ExecutorService globalExecutor();
    ActorSystem blockingSystem();
    ActorMaterializer blockingActorMaterializer();
    ExecutorService blockingExecutor();
    ActorSystem wsSystem();
    ActorMaterializer wsClientActorMaterializer();
    ExecutorService wsExecutor();
    Http wsHttp();
    ActorSystem websocketSystem();
    ActorMaterializer websocketActorMaterializer();
    ExecutorService websocketExecutor();
    Http websocketHttp();
    Mode mode();
}
