package org.reactivecouchbase.examples.webstack;

import org.reactivecouchbase.examples.webstack.controllers.HomeController;
import org.reactivecouchbase.webstrack.WebStackApp;
import scala.concurrent.duration.FiniteDuration;

import static akka.http.javadsl.model.HttpMethods.*;

public class MyApp extends WebStackApp {{

    $(GET,    "/hello",      HomeController::index);
    $(GET,    "/stream",     HomeController::stream);
    $(GET,    "/location",   HomeController::location);

}}