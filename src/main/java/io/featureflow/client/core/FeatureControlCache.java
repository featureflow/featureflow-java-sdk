package io.featureflow.client.core;
import io.featureflow.client.model.FeatureControl;

import java.io.Closeable;
import java.util.Map;
/**
 * Created by oliver.oldfieldhodge on 25/07/2015.
 * This is the repository that will hold the runtime state for features, it gets the feature from the server or local dev file
 */
public interface FeatureControlCache extends Closeable{

    void init(Map<String, FeatureControl> featureControls);
    FeatureControl get(String key);
    Map<String, FeatureControl> getAll();
    void update(String key, FeatureControl featureControl);
    void delete(String key);
}
