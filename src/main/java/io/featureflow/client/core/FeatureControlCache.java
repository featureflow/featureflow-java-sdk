package io.featureflow.client.core;
import io.featureflow.client.model.FeatureControl;

import java.io.Closeable;
import java.util.Map;
/**
 * This is the repository that will hold the runtime state for features
 */
public interface FeatureControlCache extends Closeable{

    void init(Map<String, FeatureControl> featureControls);
    FeatureControl get(String key);
    Map<String, FeatureControl> getAll();
    void update(String key, FeatureControl featureControl);
    void delete(String key);
}
