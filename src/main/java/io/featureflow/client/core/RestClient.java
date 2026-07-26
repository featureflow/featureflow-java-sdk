package io.featureflow.client.core;

import io.featureflow.client.model.Event;
import io.featureflow.client.model.Feature;

import java.io.IOException;
import java.util.List;

public interface RestClient {
    void registerFeatureControls(List<Feature> featureRegistrations) throws IOException;

    /**
     * @return the response outcome, or {@code null} if the request could not be sent
     * (network error) — callers treat that as fire-and-forget, the same as node's client.
     */
    EventsPostResult postEvents(List<? extends Event> events);
}
