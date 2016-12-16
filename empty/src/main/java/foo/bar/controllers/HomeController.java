package foo.bar.controllers;

import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.webstrack.actions.Action;

import static org.reactivecouchbase.webstrack.result.Results.*;

public class HomeController {

    public static Action index() {
        return Action.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }
}