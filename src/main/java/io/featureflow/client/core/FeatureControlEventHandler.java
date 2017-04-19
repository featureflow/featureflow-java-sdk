package io.featureflow.client.core;

import io.featureflow.client.FeatureFlowContext;

import java.util.Arrays;

/**
 * Created by oliver.oldfieldhodge on 25/2/17.
 */
public class FeatureControlEventHandler {
    private final FeatureflowRestClient featureflowRestClient;


    public FeatureControlEventHandler(FeatureflowRestClient featureflowRestClient) {
        this.featureflowRestClient = featureflowRestClient;
    }

    public void saveEvent(String featureKey, String evaluatedVariant, FeatureFlowContext context){
        FeatureEvalEvent event = new FeatureEvalEvent(featureKey, evaluatedVariant, context);
        featureflowRestClient.postFeatureEvalEvents(Arrays.asList(event));
    }
}
