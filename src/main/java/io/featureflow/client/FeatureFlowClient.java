package io.featureflow.client;

import java.io.Closeable;
import java.util.List;

/**
 * Created by oliver on 15/08/2016.
 */
public interface FeatureFlowClient extends Closeable {



    Evaluate evaluate(String featureKey, FeatureFlowContext featureFlowContext, String failoverVariant);

    Evaluate evaluate(String featureKey, String failoverVariant);

    static Builder builder(String apiKey){
        return new Builder(apiKey);
    }


    class Builder {
        private FeatureFlowConfig config = null;
        private String apiKey;
        private FeatureControlUpdateHandler featureControlUpdateHandler;
        private List<FeatureRegistration> featureRegistrations;


        public Builder (String apiKey){
            this.apiKey = apiKey;
        }

        public Builder withCallback(FeatureControlUpdateHandler featureControlUpdateHandler){
            this.featureControlUpdateHandler = featureControlUpdateHandler;
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
            return new FeatureFlowClientImpl(apiKey, featureRegistrations, config, featureControlUpdateHandler);
        }
    }

    class Evaluate {
        private final String failoverVariant;
        private final FeatureFlowClientImpl featureflowClient;
        private final String featureKey;
        private final FeatureFlowContext featureflowContext;

        Evaluate(FeatureFlowClientImpl featureFlowClient, String featureKey, FeatureFlowContext featureFlowContext, String failoverVariant) {
            this.featureflowClient = featureFlowClient;
            this.featureKey = featureKey;
            this.featureflowContext = featureFlowContext;
            this.failoverVariant = failoverVariant;

        }
        public boolean isOn(){
            return is(Variant.on);
        }
        public boolean isOff(){
            return is(Variant.off);
        }
        public boolean is(String variant){
            String result = featureflowClient.eval(featureKey, featureflowContext, failoverVariant);
            return variant.equals(result);
        }
        public String value(){
            String result = featureflowClient.eval(featureKey, featureflowContext, failoverVariant);
            return result;
        }
    }
}
