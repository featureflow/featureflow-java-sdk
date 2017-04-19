package io.featureflow.client;

import io.featureflow.client.model.FeatureControl;

/**
 * Created by oliver on 7/06/2016.
 */
public interface FeatureControlCallbackHandler {
    void onUpdate(FeatureControl control);
}
