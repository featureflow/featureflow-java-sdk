package io.featureflow.client;

/**
 * Created by oliver on 6/06/2016.
 * Based on https://github.com/aslakhellesoy/eventsource-java/blob/master/src/main/java/com/github/eventsource/client/impl/EventStreamParser.java
 * This is the handler for incoming SSE Events
 */

public interface EventSourceHandler {
    void onConnect() throws Exception;

    void onMessage(String event, MessageEvent message) throws Exception;

    void onError(Throwable var1);
}
