package io.featureflow.client;

/**
 * Created by oliver.oldfieldhodge on 25/2/17.
 */
public class FeatureEvalEvent {
    protected String featureId;
    protected String featureKey;
    protected String evaluatedVariant;
    protected FeatureFlowContext context;

    public FeatureEvalEvent(String featureId, String featureKey, String evaluatedVariant, FeatureFlowContext context) {
        this.featureId = featureId;
        this.featureKey = featureKey;
        this.evaluatedVariant = evaluatedVariant;
        this.context = context;
    }
}