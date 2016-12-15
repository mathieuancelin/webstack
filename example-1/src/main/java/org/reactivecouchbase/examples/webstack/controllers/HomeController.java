package org.reactivecouchbase.examples.webstack.controllers;

import org.reactivecouchbase.webstrack.BootstrappedContext;
import org.reactivecouchbase.webstrack.WebStackApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import akka.Done;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.stream.javadsl.Source;
import javaslang.collection.HashMap;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.webstrack.libs.concurrent.Concurrent;
import org.reactivecouchbase.webstrack.libs.ws.WS;
import org.reactivecouchbase.webstrack.libs.ws.WSResponse;
import org.reactivecouchbase.webstrack.mvc.actions.Action;
import org.reactivecouchbase.webstrack.mvc.result.Result;
import org.reactivecouchbase.webstrack.mvc.result.Results;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static akka.pattern.PatternsCS.after;
import static org.reactivecouchbase.webstrack.mvc.result.Results.*;
import static akka.http.javadsl.model.HttpMethods.*;

public class HomeController {

    public static Action index() {
        return Action.sync(ctx ->
                Ok.text("Hello World!\n")
        );
    }
}