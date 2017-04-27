package io.featureflow.client.core;

import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.model.Event;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by oliver.oldfieldhodge on 3/1/17.
 */
public class FeatureEventHandler implements Closeable {
    private final BlockingQueue<Event> eventsQueue;
    private final RestClient restClient;

    public FeatureEventHandler(FeatureflowConfig config, RestClient restClient) {
        this.eventsQueue = new ArrayBlockingQueue<Event>(10000);
        this.restClient = restClient;

    }

    public boolean sendEvent(Event event){
        restClient.postEvents(Arrays.asList(event));
        return true;

    }
    public boolean queueEvent(Event event){
        return eventsQueue.offer(event);
    }

    @Override
    public void close() throws IOException {

    }
}
