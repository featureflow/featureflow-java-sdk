package io.featureflow.client;

/**
 * The feature status is enabled or disabled, the default is set in the feature
 * A variant may be a colour or other value and cna be set against a target
 * If no variants match at all, then the default rollout strategy is checked (ie percent)
 * If the user is within the rollout strategy then the feature is enabled with the default variation
 * If the context does not match any variation and is not in the generic rollout group then the feature is disabled
 * If there is any problem or if the feature control has been set to of then the feature status takes the developer defined default
 */
public class FeatureStatus {
    private String key;
    private boolean enabled;
    private String variantValue = null;


    public FeatureStatus(String key, boolean enabled, String variantValue) {
        this.key = key;
        this.enabled = enabled;
        this.variantValue = variantValue;
    }

    public String getKey(){
        return key;
    }

    public String getVariant(){
        return variantValue;
    }

    public boolean isEnabled() {
        return enabled;
    }


    @Override
    public String toString() {
        return "FeatureStatus{" +
                "key='" + key + '\'' +
                ", enabled=" + enabled +
                ", variantValue='" + variantValue + '\'' +
                '}';
    }
}
