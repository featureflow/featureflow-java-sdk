package io.featureflow.client;

import io.featureflow.client.core.RestClient;
import io.featureflow.client.model.Event;
import io.featureflow.client.model.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class RestClientMock implements RestClient {
    public RestClientMock(String apiKey, FeatureflowConfig config) {
        log.warn("Creating offline mode rest client - offline mode enabled");
    }
    private static final Logger log = LoggerFactory.getLogger(RestClientMock.class);

    @Override
    public void registerFeatureControls(List<Feature> featureRegistrations) throws IOException {
        log.debug("Not registering controls with featureflow - offline mode enabled");
    }

    @Override
    public void postEvents(List<? extends Event> events) {
        log.debug("Not posting events - offline mode enabled");
    }
}
