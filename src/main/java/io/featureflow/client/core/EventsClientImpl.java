package io.featureflow.client.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.model.Event;

/**
 * The events client posts events back to featureflow. We queue them up
 */
public class EventsClientImpl implements EventsClient {
    private final BlockingQueue<Event> eventsQueue;
    private final RestClient restClient;
    private final long creationTime;

    public EventsClientImpl(FeatureflowConfig config, RestClient restClient) {
        this.eventsQueue = new ArrayBlockingQueue<>(10000);
        this.restClient = restClient;
        this.creationTime = System.currentTimeMillis();

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
        List<Event> events = new ArrayList<>(eventsQueue.size());
        eventsQueue.drainTo(events);
        if(!events.isEmpty()) {
            restClient.postEvents(events);
        }
    }
    @Override
    public boolean queueEvent(Event event){
        return eventsQueue.offer(event);
    }

    @Override
    public void close() throws IOException {
        // Only send queued events if the client has existed for more than 5 minutes
        // This prevents accidental DDOS if the user is managing the featureflow client singleton incorrectly
        long currentTime = System.currentTimeMillis();
        long timeSinceCreation = currentTime - creationTime;
        long fiveMinutesInMillis = 5 * 60 * 1000; // 5 minutes in milliseconds
        
        if (timeSinceCreation > fiveMinutesInMillis) {
            sendQueue();
        }
    }



}
