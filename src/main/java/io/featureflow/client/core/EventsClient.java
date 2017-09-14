package io.featureflow.client.core;

import io.featureflow.client.model.Event;

import java.io.Closeable;
import java.io.IOException;

public interface EventsClient extends Closeable {
    boolean queueEvent(Event event);
}
