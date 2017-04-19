package io.featureflow.client.core;

import io.featureflow.client.FeatureFlowContext;

/**
 * Created by oliver.oldfieldhodge on 25/2/17.
 */
public class FeatureEvalEvent {
    protected String featureKey;
    protected String evaluatedVariant;
    protected FeatureFlowContext context;

    public FeatureEvalEvent(){}
    
    public FeatureEvalEvent(String featureKey, String evaluatedVariant, FeatureFlowContext context) {
        this.featureKey = featureKey;
        this.evaluatedVariant = evaluatedVariant;
        this.context = context;
    }
}
