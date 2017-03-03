package io.featureflow.client;

import java.util.Arrays;

/**
 * Created by oliver.oldfieldhodge on 25/2/17.
 */
public class FeatureControlEventHandler {
    private final FeatureControlRestClient featureControlRestClient;


    public FeatureControlEventHandler(FeatureControlRestClient featureControlRestClient) {
        this.featureControlRestClient = featureControlRestClient;
    }

    public void saveEvent(String featureId, String featureKey, String evaluatedVariant, FeatureFlowContext context){
        FeatureEvalEvent event = new FeatureEvalEvent(featureId, featureKey, evaluatedVariant, context);
        featureControlRestClient.postFeatureEvalEvents(Arrays.asList(event));
    }
}
