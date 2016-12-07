package io.featureflow.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * Created by oliver on 25/05/2016.
 */
public interface StreamClient extends Closeable
{
    Future<Void> start();
    boolean initialized();
    void close() throws IOException;
}
