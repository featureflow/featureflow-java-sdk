package io.featureflow.client;

/**
 * Implement the user provider to provide featureflow is a user and attributes
 */
public interface FeatureflowUserProvider {
    FeatureflowUser getUser(String id);
}
