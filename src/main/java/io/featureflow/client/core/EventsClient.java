package io.featureflow.client.core;

import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.model.Event;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * The events client posts events back to featureflow. We queue them up
 */
public class EventsClient implements Closeable {
    private final BlockingQueue<Event> eventsQueue;
    private final RestClient restClient;

    public EventsClient(FeatureflowConfig config, RestClient restClient) {
        this.eventsQueue = new ArrayBlockingQueue<Event>(10000);
        this.restClient = restClient;

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        final Runnable sender = new Sender();

        executorService.scheduleAtFixedRate(new Runnable() {
            private final ExecutorService executor = Executors.newSingleThreadExecutor();
            private Future<?> lastExecution;
            @Override
            public void run() {
                if (lastExecution != null && !lastExecution.isDone()) {
                    return;
                }
                lastExecution = executor.submit(sender);
            }
        }, 10, 30, TimeUnit.SECONDS);
    }

    class Sender implements Runnable {

        @Override
        public void run() {
            sendQueue();
        }
    }
    
    private void sendQueue() {
        List<Event> events = new ArrayList(eventsQueue.size());
        eventsQueue.drainTo(events);
        if(!events.isEmpty())restClient.postEvents(events);
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
        sendQueue();
    }



}
