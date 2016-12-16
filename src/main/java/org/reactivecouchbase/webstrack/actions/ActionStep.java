package org.reactivecouchbase.webstrack.actions;

import io.undertow.server.HttpServerExchange;
import javaslang.collection.HashMap;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.webstrack.env.Env;
import org.reactivecouchbase.webstrack.result.Result;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public interface ActionStep {

    Future<Result> invoke(RequestContext request, Function<RequestContext, Future<Result>> block);

    default Future<Result> innerInvoke(RequestContext request, Function<RequestContext, Future<Result>> block) {
        try {
            return this.invoke(request, block);
        } catch (Exception e) {
            InternalActionsHelper.logger.error("innerInvoke action error", e);
            return Future.successful(InternalActionsHelper.transformError(e, request));
        }
    }

    default Action sync(Function<RequestContext, Result> block) {
        return async(req -> Future.async(() -> {
            try {
                return block.apply(req);
            } catch (Exception e) {
                InternalActionsHelper.logger.error("Sync action error", e);
                return InternalActionsHelper.transformError(e, req);
            }
        }, Env.blockingExecutor()));
    }

    default Action async(Function<RequestContext, Future<Result>> block) {
        return async(Env.blockingExecutor(), block);
    }

    default Action async(ExecutorService ec, Function<RequestContext, Future<Result>> block) {
        Function<HttpServerExchange, RequestContext> rcBuilder =
                httpServerExchange -> new RequestContext(HashMap.empty(), httpServerExchange, ec);
        return new Action(this, rcBuilder, block, ec);
    }

    default ActionStep combine(ActionStep other) {
        ActionStep that = this;
        return (request, block) -> that.innerInvoke(request, r1 -> other.innerInvoke(r1, block));
    }

    default ActionStep andThen(ActionStep other) {
        return combine(other);
    }
}
