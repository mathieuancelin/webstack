package org.reactivecouchbase.webstrack.mvc.actions;

import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.json.mapping.ThrowableWriter;
import org.reactivecouchbase.webstrack.mvc.result.Result;
import org.reactivecouchbase.webstrack.mvc.result.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InternalActionsHelper {

    static final Logger logger = LoggerFactory.getLogger(InternalActionsHelper.class);

    static final ActionStep EMPTY = (request, block) -> {
        try {
            return block.apply(request);
        } catch (Exception e) {
            logger.error("Empty action error", e);
            return Future.successful(transformError(e, request));
        }
    };

    static Result transformError(Throwable t, RequestContext request) {
        // always return JSON for now
        return Results.InternalServerError.json(Json.obj().with("error",
                new ThrowableWriter(true).write(t)));
    }
}
