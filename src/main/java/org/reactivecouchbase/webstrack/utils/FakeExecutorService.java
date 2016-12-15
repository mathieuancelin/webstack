package org.reactivecouchbase.webstrack.utils;

import org.reactivecouchbase.concurrent.Promise;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class FakeExecutorService implements ExecutorService {

    private final Executor executor;

    public FakeExecutorService(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Promise<T> promise = Promise.create();
        execute(() -> {
            try {
                promise.trySuccess(task.call());
            } catch (Exception e) {
                promise.tryFailure(e);
            }
        });
        return promise.future().toJdkFuture();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Promise<T> promise = Promise.create();
        execute(() -> {
            try {
                task.run();
                promise.trySuccess(result);
            } catch (Exception e) {
                promise.tryFailure(e);
            }
        });
        return promise.future().toJdkFuture();
    }

    @Override
    public Future<?> submit(Runnable task) {
        Promise<Object> promise = Promise.create();
        execute(() -> {
            try {
                task.run();
                promise.trySuccess(null);
            } catch (Exception e) {
                promise.tryFailure(e);
            }
        });
        return promise.future().toJdkFuture();
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new RuntimeException("Not Supported Yet !!!");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new RuntimeException("Not Supported Yet !!!");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new RuntimeException("Not Supported Yet !!!");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new RuntimeException("Not Supported Yet !!!");
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }
}