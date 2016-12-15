# webstack

`webstack` is an experiment to explore the setup of a JVM based async/streamed web stack.

## Routes

```java
public class MyApp extends WebStackApp {
    public void defineRoutes() {
        $(GET,       "/hello",        MyController::index);
        $(GET,       "/sse",          MyController::testStream);
    }
}
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

    public static class BaseController {

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
            return Action.sync(ctx ->
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

## Use it in your project

in your `build.gradle` file


```groovy
repositories {
    mavenCentral()
    maven {
        url 'https://raw.github.com/mathieuancelin/webstack/master/repository/releases/'
    }
}

dependencies {
    compile("org.reactivecouchbase.webstack:webstack-core:0.1.0")
}
```
