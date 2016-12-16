package org.reactivecouchbase.webstrack.actions;

import io.undertow.server.HttpServerExchange;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.webstrack.result.Result;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class Action {

    final ActionStep actionStep;
    final Function<HttpServerExchange, RequestContext> rcBuilder;
    final Function<RequestContext, Future<Result>> block;
    final ExecutorService ec;

    Action(ActionStep actionStep, Function<HttpServerExchange, RequestContext> rcBuilder, Function<RequestContext, Future<Result>> block, ExecutorService ec) {
        this.actionStep = actionStep;
        this.rcBuilder = rcBuilder;
        this.block = block;
        this.ec = ec;
    }

    public Future<Result> run(HttpServerExchange httpServerExchange) {
        try {
            RequestContext rc = rcBuilder.apply(httpServerExchange);
            Future<Result> result = actionStep.innerInvoke(rc, block);
            return result.recoverWith(t -> Future.successful(InternalActionsHelper.transformError(t, rc)), ec);
        } catch (Exception e) {
            return Future.failed(e);
        }
    }

    public Action withExecutor(ExecutorService ec) {
        return new Action(actionStep, rcBuilder, block, ec);
    }

    public static Action sync(Function<RequestContext, Result> block) {
        return InternalActionsHelper.EMPTY.sync(block);
    }

    public static Action async(Function<RequestContext, Future<Result>> block) {
        return InternalActionsHelper.EMPTY.async(block);
    }

    public static Action async(ExecutorService ec, Function<RequestContext, Future<Result>> block) {
        return InternalActionsHelper.EMPTY.async(ec, block);
    }
}
