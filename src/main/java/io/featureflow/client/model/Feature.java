package io.featureflow.client.model;

import java.util.List;

/**
 * This is used to actively register available features in the featureflowClient
 * A feature can declare its key and available variants
 * If the feature does not exist in featureflow then it will be created with the defined variants
 * If no variants are defined then a default on/off feature will be created
 * If a feature does match then the variants will be merged and the feature will be shown as available in the clients configured environment
 */
public class Feature {
    public final List<Variant> variants;
    public final String key;
    public final String failoverVariant;

    public Feature(String key) {
        this.key = key;
        this.variants = null; //if null then we check for existing variants in config or default to on/off
        this.failoverVariant = Variant.off;
    }
    public Feature(String key, String failoverVariant) {
        this.key = key;
        this.variants = null;
        this.failoverVariant = failoverVariant;
    }
    public Feature(String key, List<Variant> variants) {
        this.key = key;
        this.variants = variants;
        this.failoverVariant = Variant.off;
    }
    public Feature(String key, List<Variant> variants, String failoverVariant) {
        this.key = key;
        this.variants = variants;
        this.failoverVariant = failoverVariant;
    }
}
