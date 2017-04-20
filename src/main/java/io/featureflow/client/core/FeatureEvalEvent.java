package io.featureflow.client.core;

import io.featureflow.client.FeatureflowContext;

/**
 * Created by oliver.oldfieldhodge on 25/2/17.
 */

public class FeatureEvalEvent {
    public String featureKey;
    public String evaluatedVariant;
    public FeatureflowContext context;

    public FeatureEvalEvent(){}
    
    public FeatureEvalEvent(String featureKey, String evaluatedVariant, FeatureflowContext context) {
        this.featureKey = featureKey;
        this.evaluatedVariant = evaluatedVariant;
        this.context = context;
    }
}
