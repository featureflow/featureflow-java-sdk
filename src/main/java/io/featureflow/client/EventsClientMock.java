package io.featureflow.client;

import io.featureflow.client.core.EventsClient;
import io.featureflow.client.core.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class EventsClientMock implements EventsClient {
    private static final Logger log = LoggerFactory.getLogger(EventsClientMock.class);

    public EventsClientMock(FeatureflowConfig config, RestClient restClient) {
        log.warn("Events will not be sent to featureflow - offline mode enabled");
    }

    @Override
    public void evaluateEvent(String featureKey, String evaluatedVariant, FeatureflowUser user, boolean trackEvents) {
        log.debug("Evaluate event for {} not sent to featureflow - offline mode enabled", featureKey);
    }

    @Override
    public void trackEvent(String goalKey, FeatureflowUser user, Object details) {
        log.debug("Goal event {} not sent to featureflow - offline mode enabled", goalKey);
    }

    @Override
    public void applyServerConfig(Map<String, Object> config) {
    }

    @Override
    public void sendQueue() {
    }

    @Override
    public void close() throws IOException {
    }
}
