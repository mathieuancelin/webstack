package org.reactivecouchbase.examples.webstack.controllers;

import akka.stream.javadsl.Source;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.webstrack.libs.ws.WS;
import org.reactivecouchbase.webstrack.libs.ws.WSResponse;
import org.reactivecouchbase.webstrack.mvc.actions.Action;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static org.reactivecouchbase.webstrack.mvc.result.Results.*;

public class HomeController {

    public static Action index() {
        return Action.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }

    public static Action stream() {
        return Action.sync(ctx ->
            Ok.stream(
                Source.tick(
                    FiniteDuration.apply(0, TimeUnit.MILLISECONDS),
                    FiniteDuration.apply(100, TimeUnit.MILLISECONDS),
                    ""
                )
                .map(l -> Json.obj().with("time", System.currentTimeMillis()).with("message", "Hello World!"))
                .map(Json::stringify)
                .map(j -> "data: " + j + "\n\n")
            ).as("text/event-stream")
        );
    }

    public static Action location() {
        return Action.async(ctx ->
            WS.host("http://freegeoip.net").withPath("/json/")
                .call()
                .flatMap(WSResponse::body)
                .map(r -> r.json().pretty())
                .map(p -> Ok.json(p))
        );
    }
}