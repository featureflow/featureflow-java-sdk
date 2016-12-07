package io.featureflow.client;

import java.io.Closeable;
import java.util.List;

/**
 * Created by oliver on 15/08/2016.
 */
public interface FeatureFlowClient extends Closeable {

    String evaluate(String featureKey, FeatureFlowContext featureFlowContext, String failoverVariant);

    String evaluate(String featureKey, String failoverVariant);

    static Builder builder(String apiKey){
        return new Builder(apiKey);
    }

    class Builder {
        private FeatureFlowConfig config = null;
        private String apiKey;
        private FeatureControlEventHandler featureControlEventHandler;
        private List<FeatureRegistration> featureRegistrations;

        public Builder (String apiKey){
            this.apiKey = apiKey;
        }

        public Builder withCallback(FeatureControlEventHandler featureControlEventHandler){
            this.featureControlEventHandler = featureControlEventHandler;
            return this;
        }

        public Builder withConfig(FeatureFlowConfig config){
            this.config = config;
            return this;
        }

        public Builder withFeatureRegistrations(List<FeatureRegistration> featureRegistrations){
            this.featureRegistrations = featureRegistrations;
            return this;
        }

        public FeatureFlowClient build(){
            if(config==null){ config = new FeatureFlowConfig.Builder().build();}
            return new FeatureFlowClientImpl(apiKey, featureRegistrations, config, featureControlEventHandler);
        }
    }

}
