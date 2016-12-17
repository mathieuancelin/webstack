# webstack

`webstack` is an experiment to explore the setup of a JVM based async/streamed web stack.

## Routes

```java
public class MyApp extends WebStackApp {{

    $(GET,       "/hello",        MyController::index);
    $(GET,       "/sse",          MyController::testStream);
    $(GET,       "/ws",           MyController::pingWebSocket);

}}
```

just use the right main class

```groovy
mainClassName = 'org.reactivecouchbase.webstrack.WebStack'
```

## Actions

Every HTTP resource returns an `Action`

```java

public class MyController {

    public static Action text() {
        return Action.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }
}
```

`Actions`s can easily be composed from `ActionStep`s

```java

public class MyController {

    // ActionStep that logs before request
    private static ActionStep LogBefore = (req, block) -> {
        Long start = System.currentTimeMillis();
        logger.info("[Log] before actionStep -> {}", req.getRequest().getRequestURI());
        return block.apply(req.setValue("start", start));
    };

    // ActionStep that logs after request
    private static ActionStep LogAfter = (req, block) -> block.apply(req).andThen(ttry -> {
        logger.info(
            "[Log] after actionStep -> {} : took {}",
            req.getRequest().getRequestURI(),
            Duration.of(
                System.currentTimeMillis() - req.getValue("start", Long.class),
                TimeUnit.MILLISECONDS
            ).toHumanReadable()
        );
    });

    // previous ActionSteps composition as a new one
    private static ActionStep LoggedAction = LogBefore.andThen(LogAfter);

    public Action text() {
        // Use composed actionStep
        return LoggedAction.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }
}
```

## Examples

```java
package example;

public class App extends WebStackApp {

    public void defineRoutes() {
        $(GET, "/hello",    SSEController::text);
        $(GET, "/json",     SSEController::json);
        $(GET, "/location", SSEController::fetchLocation);
    }

    public static class SSEController {

        private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

        // Action that logs before request
        private static ActionStep LogBefore = (req, block) -> {
            Long start = System.currentTimeMillis();
            logger.info("[Log] before action -> {}", req.getRequest().getRequestURI());
            return block.apply(req.setValue("start", start));
        };

        // Action that logs after request
        private static ActionStep LogAfter = (req, block) -> block.apply(req).andThen(ttry -> {
            logger.info(
                "[Log] after action -> {} : took {}",
                req.getRequest().getRequestURI(),
                Duration.of(
                    System.currentTimeMillis() - req.getValue("start", Long.class),
                    TimeUnit.MILLISECONDS
                ).toHumanReadable()
            );
        });

        // Actions composition
        private static ActionStep LoggedAction = LogBefore.andThen(LogAfter);

        public Action text() {
            // Use composed action
            return LoggedAction.sync(ctx ->
                Ok.text("Hello World!\n")
            );
        }

        public Action json() {
            return LoggedAction.sync(ctx ->
                Ok.json(
                    Json.obj().with("message", "Hello World!")
                )
            );
        }

        // Here, async action that fetch a remote webservice and returns the content
        public Action fetchLocation() {
            return LoggedAction.async(ctx ->
                WS.host("http://freegeoip.net")
                    .withPath("/json/")
                    .call()
                    .flatMap(WSResponse::body)
                    .map(r -> r.json().pretty())
                    .map(p -> Ok.json(p))
            );
        }
    }
}
```

### SSE

```java
package example;

public class SSEApp extends WebStackApp {

    public void defineRoutes() {
        $(GET, "/stream", SSEController::sseStream);
    }

    public static class SSEController {

        // Stream a json chunk each second forever
        public static Action sseStream() {
            return Action.sync(ctx ->
                Ok.stream(
                    Source.tick(
                        FiniteDuration.Zero(),
                        FiniteDuration.apply(1, TimeUnit.SECONDS),
                        ""
                    )
                    .map(l -> Json.obj().with("time", System.currentTimeMillis()).with("value", l))
                    .map(Json::stringify)
                    .map(j -> "data: " + j + "\n\n")
                ).as("text/event-stream")
            );
        }
    }
}
```

## WebSocket

```java
package example;

public class WebSocketApp extends WebStackApp {

    public void defineRoutes() {
        $(GET,   "/simple",   WebSocketController::simpleWebsocket);
        $(GET,   "/ping",     WebSocketController::webSocketPing);
    }

    public static class WebSocketController {

        private final static Logger logger = LoggerFactory.getLogger(WebSocketController.class);

        // Use a flow to handle input and output
        public WebSocketAction simpleWebsocket() {
            return WebSocketAction.accept(ctx ->
                Flow.fromSinkAndSource(
                    Sink.foreach(msg -> logger.info(msg.asTextMessage().getStrictText())),
                    Source.tick(
                        FiniteDuration.Zero(),
                        FiniteDuration.create(10, TimeUnit.MILLISECONDS),
                        TextMessage.create(Json.obj().with("msg", "Hello World!").stringify())
                    )
                )
            );
        }

        // Use an actor to handle input and output
        @WebSocketMapping(path = "/ping")
            return WebSocketAction.accept(context ->
                ActorFlow.actorRef(
                    out -> WebsocketPing.props(context, out)
                )
            );
        }
    }

    private static class WebsocketPing extends UntypedActor {

        private final ActorRef out;
        private final WebSocketContext ctx;
        private static final Logger logger = LoggerFactory.getLogger(WebsocketPing.class);

        public WebsocketPing(WebSocketContext ctx, ActorRef out) {
            this.out = out;
            this.ctx = ctx;
        }

        public static Props props(WebSocketContext ctx, ActorRef out) {
            return Props.create(WebsocketPing.class, () -> new WebsocketPing(ctx, out));
        }

        public void onReceive(Object message) throws Exception {
            logger.info("[WebsocketPing] received message from the client {}", message);
            if (message instanceof Message) {
                logger.info("[WebsocketPing] Sending message back the client");
                out.tell(message, getSelf());
            } else {
                unhandled(message);
            }
        }
    }
}
```

## Use it for a project

Just clone the `empty` directory from this repo. It's a ready to work `webstack` application waiting for you code.

It is using [devloop](https://github.com/criteo/loop) to enhance development flow.
To install it just `npm install -g devloop` then run `loop` in the app.

The code of the app is organized this way

* app
  * controllers
    * HomeController.java
  * Routes.java
* res
  * application.conf
  * logback.xml
  * templates
    * index.html
  * public
    * images
    * js
      * main.js
    * css
      * main.css
* tests
  * AppTest.java

## Use it in your project

in your `build.gradle` file


```groovy
repositories {
    mavenCentral()
    maven {
        url 'https://raw.github.com/mathieuancelin/webstack/master/repository/snapshots/'
    }
}

dependencies {
    compile("org.reactivecouchbase.webstack:webstack-core:0.1.0-SNAPSHOT")
}
```
