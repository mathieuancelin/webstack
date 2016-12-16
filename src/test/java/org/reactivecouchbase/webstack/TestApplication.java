package org.reactivecouchbase.webstack;

import org.reactivecouchbase.webstrack.BootstrappedContext;
import org.reactivecouchbase.webstrack.WebStackApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.http.javadsl.model.HttpMethods.*;

public class TestApplication {

    public static BootstrappedContext run() {
        return new WebStackApp() {{

            $(GET,       "/sayhello",          TestController::index);
            $(GET,       "/sse",               TestController::testStream);
            $(GET,       "/sse2",              TestController::testStream2);
            $(GET,       "/test",              TestController::text);
            $(GET,       "/huge",              TestController::hugeText);
            $(GET,       "/json",              TestController::json);
            $(GET,       "/html",              TestController::html);
            $(GET,       "/template",          TestController::template);
            $(GET,       "/ws",                TestController::testWS);
            $(GET,       "/ws2",               TestController::testWS2);
            $(GET,       "/hello/{name}",      TestController::hello);
            $(POST,      "/post",              TestController::testPost);
            $(GET,       "/websocketping",     TestController::webSocketPing);
            $(GET,       "/websocketsimple",   TestController::simpleWebsocket);
            $(GET,       "/websocket/{id}",    TestController::webSocketWithContext);

        }}.startApp();
    }

    public static void main(String... args) {
        run();
    }
}
