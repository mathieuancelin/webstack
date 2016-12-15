package org.reactivecouchbase.webstrack.libs.ws;

import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectionContext;
import akka.http.javadsl.Http;
import akka.stream.ActorMaterializer;
import com.google.common.base.Throwables;
import com.typesafe.config.ConfigFactory;
import org.reactivecouchbase.webstrack.config.Configuration;
import org.reactivecouchbase.webstrack.utils.FakeExecutorService;

import javax.net.ssl.SSLContext;

class InternalWSHelper {

    static final ActorSystem wsSystem = ActorSystem.create("ws-system",
            Configuration.current().underlying.atPath("webstack.systems.ws").withFallback(ConfigFactory.empty()));
    static final ActorMaterializer wsClientActorMaterializer = ActorMaterializer.create(wsSystem);
    static final FakeExecutorService wsExecutor = new FakeExecutorService(wsSystem.dispatcher());
    static final Http wsHttp = Http.get(wsSystem);

    static {
        try {
            SSLContext ctx = SSLContext.getDefault();
            wsHttp.setDefaultClientHttpsContext(ConnectionContext.https(ctx));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            wsSystem.terminate();
        }));
    }
}
