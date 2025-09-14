package io.featureflow.client.example;

import java.util.Arrays;
import java.util.List;

import io.featureflow.client.FeatureControlCallbackHandler;
import io.featureflow.client.FeatureflowClient;
import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.FeatureflowUser;
import io.featureflow.client.model.Feature;
import io.featureflow.client.model.FeatureControl;

/**
 * Example demonstrating the FeatureflowPollingClient with callbacks.
 * This demo runs for 60 seconds to show polling behavior and feature update callbacks.
 * The polling client is now the default behavior unless streaming is explicitly enabled.
 */
public class PollingClientExample {
    
    /**
     * Callback handler that prints feature updates
     */
    static class FeatureUpdateCallback implements FeatureControlCallbackHandler {
        @Override
        public void onUpdate(FeatureControl control) {
            System.out.println("🔄 Feature Updated: " + control.key + 
                             " (enabled: " + control.enabled + 
                             ", offVariant: " + control.offVariantKey + ")");
        }
    }
    
    /**
     * Callback handler that prints feature deletions
     */
    static class FeatureDeleteCallback implements FeatureControlCallbackHandler {
        @Override
        public void onUpdate(FeatureControl control) {
            System.out.println("🗑️ Feature Deleted: " + control.key);
        }
    }
    
    public static void main(String[] args) {
        // Polling Behavior Demo
        System.out.println("=== Featureflow Polling Client Demo ===");
        System.out.println("This example will run for 60 seconds to demonstrate polling behavior.");
        System.out.println("Watch for feature update callbacks as the client polls the server...");
        
        List<Feature> features = Arrays.asList(
            new Feature("my-first-feature", "red")
        );
        FeatureflowConfig pollingConfig = FeatureflowConfig.builder()
            .withPollingInterval(20) // Poll every 20 seconds for demo (minimum allowed)
            .build();
        FeatureflowClient client = FeatureflowClient.builder("sdk-srv-env-mykey")
            .withConfig(pollingConfig)
            .withUpdateCallback(new FeatureUpdateCallback())
            .withDeleteCallback(new FeatureDeleteCallback())
            .withFeatures(features)
            .build();
        
        FeatureflowUser user = new FeatureflowUser("user123");
        
        // Run for 60 seconds to see polling in action
        try {
            for (int i = 0; i < 3; i++) { // 3 iterations * 20 seconds = 60 seconds
                Thread.sleep(20000); // Sleep to demonstrate polling behavior - in real apps this would be handled by the polling client
                System.out.println("⏰ Polling cycle " + (i + 1) + " - checking for feature updates...");
                
                // Evaluate a feature to see current state
                String currentVariant = client.evaluate("my-first-feature", user).value();
                System.out.println("   Current variant for 'my-first-feature': " + currentVariant);
            }
        } catch (InterruptedException e) {
            System.out.println("Demo interrupted");
            Thread.currentThread().interrupt();
        }
        
        // Clean up
        try {
            client.close();
        } catch (java.io.IOException e) {
            System.err.println("Error closing client: " + e.getMessage());
        }
        
        System.out.println("\n✅ Demo completed! Check the output above for feature update callbacks.");
    }
}
