package io.featureflow.client.core;

import io.featureflow.client.model.Event;

import java.io.Closeable;

public interface EventsClient extends Closeable {
    boolean queueEvent(Event event);
}
