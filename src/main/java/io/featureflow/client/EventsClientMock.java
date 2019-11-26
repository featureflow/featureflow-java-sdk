package io.featureflow.client;

import io.featureflow.client.core.EventsClient;
import io.featureflow.client.core.RestClient;
import io.featureflow.client.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EventsClientMock implements EventsClient {
    private static final Logger log = LoggerFactory.getLogger(EventsClientMock.class);

    public EventsClientMock(FeatureflowConfig config, RestClient restClient) {
        log.warn("Events will not be sent to featureflow - offline mode enabled");
    }

    @Override
    public boolean queueEvent(Event event) {
        log.debug("Event {} received but not sent to featureflow - offline mode enabled", event.toString());
        return true;
    }

    @Override
    public void close() throws IOException {
    }
}
