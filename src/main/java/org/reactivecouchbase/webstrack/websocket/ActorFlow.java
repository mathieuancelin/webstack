package org.reactivecouchbase.webstrack.websocket;

import akka.NotUsed;
import akka.actor.*;
import akka.japi.Pair;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.*;
import org.reactivecouchbase.webstrack.env.Env;
import org.reactivestreams.Publisher;

import java.util.function.Function;

public class ActorFlow {

    public static <In, Out> Flow<In, Out, NotUsed> actorRef(Function<ActorRef, Props> props) {
        return actorRef(
                props,
                1000,
                OverflowStrategy.dropNew(),
                Env.websocketSystem(),
                Env.websocketActorMaterializer()
        );
    }

    public static <In, Out> Flow<In, Out, NotUsed> actorRef(
            Function<ActorRef, Props> props,
            int bufferSize) {
        return actorRef(
                props,
                bufferSize,
                OverflowStrategy.dropNew(),
                Env.websocketSystem(),
                Env.websocketActorMaterializer()
        );
    }

    public static <In, Out> Flow<In, Out, NotUsed> actorRef(
            Function<ActorRef, Props> props,
            int bufferSize,
            OverflowStrategy overflowStrategy) {
        return actorRef(
                props,
                bufferSize,
                overflowStrategy,
                Env.websocketSystem(),
                Env.websocketActorMaterializer()
        );
    }

    public static <In, Out> Flow<In, Out, NotUsed> actorRef(
            Function<ActorRef, Props> props,
            int bufferSize,
            OverflowStrategy overflowStrategy,
            ActorRefFactory factory,
            Materializer mat) {

        Pair<ActorRef, Publisher<Out>> pair =
                Source.<Out>actorRef(bufferSize, overflowStrategy).toMat(Sink.asPublisher(AsPublisher.WITHOUT_FANOUT), Keep.both()).run(mat);

        return Flow.fromSinkAndSource(
                Sink.actorRef(
                        factory.actorOf(Props.create(WebsocketFlowActor.class, () -> new WebsocketFlowActor(props, pair.first()))),
                        new Status.Success(new Object())
                ),
                Source.fromPublisher(pair.second())
        );
    }

    private static class WebsocketFlowActor extends UntypedActor {

        private final ActorRef flowActor;

        public WebsocketFlowActor(Function<ActorRef, Props> props, ActorRef ref) {
            flowActor = context().watch(context().actorOf(props.apply(ref), "flowActor"));
        }

        @Override
        public void onReceive(Object message) throws Throwable {
            if (message instanceof Status.Success) {
                flowActor.tell(PoisonPill.getInstance(), getSelf());
            } else if (message instanceof Terminated) {
                context().stop(getSelf());
            } else {
                flowActor.tell(message, getSelf());
            }
        }

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return SupervisorStrategy.stoppingStrategy();
        }
    }
}