package io.featureflow.client;

import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;

import java.util.concurrent.Future;

public class NoOpFuture extends BasicFuture<Void> {

  public NoOpFuture() {
    super(new NoOpFutureCallback());
  }

  static class NoOpFutureCallback implements FutureCallback<Void> {
    @Override
    public void completed(Void result) {
    }

    @Override
    public void failed(Exception ex) {
    }

    @Override
    public void cancelled() {
    }
  }
}
