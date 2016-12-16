package org.reactivecouchbase.webstack;

import akka.Done;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.http.javadsl.model.ws.TextMessage;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import javaslang.collection.HashMap;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.webstrack.env.Env;
import org.reactivecouchbase.webstrack.libs.ws.WS;
import org.reactivecouchbase.webstrack.libs.ws.WSResponse;
import org.reactivecouchbase.webstrack.mvc.actions.Action;
import org.reactivecouchbase.webstrack.mvc.result.Result;
import org.reactivecouchbase.webstrack.mvc.result.Results;
import org.reactivecouchbase.webstrack.websocket.ActorFlow;
import org.reactivecouchbase.webstrack.websocket.WebSocketAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static akka.pattern.PatternsCS.after;
import static org.reactivecouchbase.webstrack.mvc.result.Results.*;

public class TestController {

    private final static Logger logger = LoggerFactory.getLogger(TestController.class);

    public static Action index() {
        return Action.sync(ctx ->
            Results.Ok.text("Hello World!\n")
        );
    }

    public static Action sseStream() {
        return Action.sync(ctx ->
            Results.Ok.stream(
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

    public static Action testStream() {
        return Action.sync(ctx -> {

            Result result = Ok.stream(
                    Source.tick(
                            FiniteDuration.apply(0, TimeUnit.MILLISECONDS),
                            FiniteDuration.apply(100, TimeUnit.MILLISECONDS),
                            ""
                    )
                            .map(l -> Json.obj().with("time", System.currentTimeMillis()).with("value", l))
                            .map(Json::stringify)
                            .map(j -> "data: " + j + "\n\n")
            ).as("text/event-stream");

            result.materializedValue(Cancellable.class).andThen(ttry -> {
                for (Cancellable c : ttry.asSuccess()) {
                    after(
                            FiniteDuration.create(500, TimeUnit.MILLISECONDS),
                            Env.globalActorSystem().scheduler(),
                            Env.globalActorSystem().dispatcher(),
                            CompletableFuture.completedFuture(Done.getInstance())
                    ).thenAccept(d ->
                            c.cancel()
                    );
                }
            });

            return result;
        });
    }

    public static Action testStream2() {
        return Action.sync(ctx -> {
            Result result = Ok.stream(SSEActor.source()).as("text/event-stream");
            result.materializedValue(ActorRef.class).andThen(ttry -> {
                for (ActorRef ref : ttry.asSuccess()) {
                    ref.tell("START", ActorRef.noSender());
                }
            });
            return result;
        });
    }

    public static Action text() {
        return Action.sync(ctx ->
                Ok.text("Hello World!\n")
        );
    }

    public static Action hello() {
        return Action.sync(ctx ->
                Ok.text("Hello " + ctx.pathParam("name").getOrElse("Unknown") + "!\n")
        );
    }

    public static Action hugeText() {
        return Action.sync(ctx ->
                Ok.text(VERY_HUGE_TEXT + "\n")
        );
    }

    public static Action json() {
        return Action.sync(ctx ->
                Ok.json(Json.obj().with("message", "Hello World!"))
        );
    }

    public static Action html() {
        return Action.sync(ctx ->
                Ok.html("<h1>Hello World!</h1>")
        );
    }

    public static Action template() {
        return Action.sync(ctx ->
                Ok.template("hello", HashMap.<String, String>empty().put("name", "Mathieu"))
        );
    }

    public static Action testPost() {
        return Action.async(ctx ->
                ctx.body()
                        .map(body -> body.asJson().asObject())
                        .map(payload -> payload.with("processed_by", "SB"))
                        .map(Ok::json)
        );
    }

    public static Action testWS() {
        return Action.async(ctx ->
                WS.host("http://freegeoip.net").withPath("/json/")
                        .call()
                        .flatMap(WSResponse::body)
                        .map(r -> r.json().pretty())
                        .map(p -> Ok.json(p))
        );
    }

    public static Action testWS2() {
        return Action.async(ctx ->
                WS.host("http://freegeoip.net")
                        .withPath("/json/")
                        .withHeader("Sent-At", System.currentTimeMillis() + "")
                        .call()
                        .flatMap(WSResponse::body)
                        .map(r -> r.json().pretty())
                        .map(p -> Ok.json(p))
        );
    }

    public static WebSocketAction simpleWebsocket() {
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

    public static WebSocketAction webSocketPing() {
        return WebSocketAction.accept(context ->
            ActorFlow.actorRef(
                out -> WebsocketPing.props(context, out)
            )
        );
    }

    public static WebSocketAction webSocketWithContext() {
        return WebSocketAction.accept(context ->
            ActorFlow.actorRef(
                out -> MyWebSocketActor.props(context, out)
            )
        );
    }

    public final static String HUGE_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum rhoncus ultrices neque, nec consectetur ex molestie et. Integer dolor purus, laoreet vel condimentum vel, pulvinar at augue. Quisque tempor ac nisl vitae faucibus. Nunc placerat lacus dolor, nec finibus nibh semper eget. Nullam ac ipsum egestas, porttitor leo eget, suscipit risus. Donec sit amet est at erat pellentesque condimentum eu quis mauris. Aliquam tristique consectetur neque, a euismod magna mattis in. Nullam ac orci lectus. Interdum et malesuada fames ac ante ipsum primis in faucibus. Curabitur iaculis, mauris non tempus sagittis, eros nisl maximus quam, sed euismod sapien est id nisl. Nulla vitae enim dictum, tincidunt lorem nec, posuere arcu. Nulla tempus elit eu magna euismod maximus. Morbi varius nulla velit, eget pulvinar augue gravida eu.\n" +
            "Curabitur enim nisl, sollicitudin at odio laoreet, finibus gravida tellus. Nulla auctor urna magna, non egestas eros dignissim sollicitudin. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Nullam eget magna sit amet magna venenatis consequat vel vel lectus. Morbi fringilla pulvinar diam sed fermentum. Praesent ac tincidunt urna. Praesent in mi dolor. Curabitur posuere massa quis lectus fringilla, at congue ante faucibus. Mauris massa lacus, egestas quis consequat ac, pretium quis arcu. Fusce placerat vel massa eu blandit.\n" +
            "Curabitur fermentum, ante a tristique interdum, enim diam pulvinar urna, nec aliquet tellus lectus id lectus. Integer ullamcorper lacinia est vulputate pretium. In a dictum velit. In mattis justo sollicitudin iaculis iaculis. Quisque suscipit lorem vel felis accumsan, quis lobortis diam imperdiet. Nullam ornare metus massa, rutrum ullamcorper metus scelerisque a. Nullam finibus diam magna, et fringilla dui faucibus vel. Etiam semper libero sit amet ullamcorper consectetur. Curabitur velit ipsum, cursus sit amet justo eget, rhoncus congue enim. In elit ex, sodales vel odio non, ultricies egestas risus. Proin venenatis consectetur augue, et vestibulum leo dictum vel. Etiam id risus vitae dolor viverra blandit ut ac ante.\n" +
            "Quisque a nibh sem. Nulla facilisi. Ut gravida, dui et malesuada interdum, nunc arcu eleifend ligula, quis ornare tortor quam at ante. Vestibulum ac porta nibh, vitae imperdiet erat. Pellentesque nec lacus ex. Nullam sed hendrerit lacus. Curabitur varius sem sit amet tortor sollicitudin auctor. Donec eu feugiat enim, quis pellentesque urna. Morbi finibus fermentum varius. Aliquam quis efficitur nisi. Cras at tortor erat. Vestibulum interdum diam lacus, a lacinia mauris dapibus ut. Suspendisse potenti.\n" +
            "Vestibulum vel diam nec felis sodales porta nec sit amet eros. Quisque sit amet molestie risus. Pellentesque turpis ante, aliquam at urna vel, pulvinar fermentum massa. Proin posuere eu erat id condimentum. Nulla imperdiet erat a varius laoreet. Curabitur sollicitudin urna non commodo condimentum. Ut id ligula in ligula maximus pulvinar et id eros. Fusce et consequat orci. Maecenas leo sem, tristique quis justo nec, accumsan interdum quam. Nunc imperdiet scelerisque iaculis. Praesent sollicitudin purus et purus porttitor volutpat. Duis tincidunt, ipsum vel dignissim imperdiet, ligula nisi ultrices velit, at sodales felis urna at mi. Donec arcu ligula, pulvinar non posuere vel, accumsan eget lorem. Vivamus ac iaculis enim, ut rutrum felis. Praesent non ultrices nibh. Proin tristique, nibh id viverra varius, orci nisi faucibus turpis, quis suscipit sem nisi eu purus.";

    public final static String VERY_HUGE_TEXT = IntStream.rangeClosed(1, 1000).mapToObj(a -> HUGE_TEXT).collect(Collectors.joining("\n"));
}
