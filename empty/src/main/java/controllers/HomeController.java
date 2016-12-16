package controllers;

import javaslang.collection.HashMap;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.webstrack.actions.Action;

import static org.reactivecouchbase.webstrack.result.Results.*;

public class HomeController {

    public static Action index() {
        return Action.sync(ctx ->
            Ok.template("index",
                    HashMap.<String, String>empty().put("who", ctx.queryParam("who").getOrElse("World")))
        );
    }
}