package io.featureflow.client.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Updated to use standard Java concurrency APIs instead of HttpClient-specific ones
 */
public class NoOpFuture implements Future<Void> {

    private final CompletableFuture<Void> future;

    public NoOpFuture() {
        this.future = new CompletableFuture<>();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    public void completed(Void result) {
        future.complete(result);
    }

    public void failed(Exception ex) {
        future.completeExceptionally(ex);
    }

    public void cancelled() {
        future.cancel(true);
    }
}
