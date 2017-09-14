package io.featureflow.client.core;

import io.featureflow.client.model.Event;
import io.featureflow.client.model.Feature;

import java.io.IOException;
import java.util.List;

public interface RestClient {
    void registerFeatureControls(List<Feature> featureRegistrations) throws IOException;
    void postEvents(List<? extends Event> events);
}
