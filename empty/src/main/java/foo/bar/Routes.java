package foo.bar.webstack;

import foo.bar.controllers.HomeController;
import org.reactivecouchbase.webstrack.WebStackApp;

import static akka.http.javadsl.model.HttpMethods.*;

public class Routes extends WebStackApp {{

    $(GET,    "/",      HomeController::index);

}}