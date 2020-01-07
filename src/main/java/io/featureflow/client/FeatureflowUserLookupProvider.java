package io.featureflow.client;

/**
 * User provider to provide featureflow with a user and attributes
 */
public interface FeatureflowUserLookupProvider {
    FeatureflowUser getUser(String userId);
}
