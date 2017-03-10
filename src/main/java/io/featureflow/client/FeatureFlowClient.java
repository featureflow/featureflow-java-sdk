package io.featureflow.client;

import java.io.Closeable;
import java.util.List;

/**
 * Created by oliver on 15/08/2016.
 */
public interface FeatureFlowClient<E extends Enum<E>> extends Closeable {



    Evaluate evaluate(String featureKey, FeatureFlowContext featureFlowContext);

    Evaluate evaluate(String featureKey);

    static Builder builder(String apiKey){
        return new Builder(apiKey);
    }



    class Builder {
        private FeatureFlowConfig config = null;
        private String apiKey;
        private FeatureControlUpdateHandler featureControlUpdateHandler;
        private List<Feature> features;



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


        public Builder withFeatures(List<Feature> features){
            this.features = features;
            return this;
        }

        public FeatureFlowClient build(){
            if(config==null){ config = new FeatureFlowConfig.Builder().build();}
            return new FeatureFlowClientImpl(apiKey, features, config, featureControlUpdateHandler);
        }
    }

    class Evaluate {
        private final FeatureFlowClientImpl featureflowClient;
        private final String featureKey;
        private final FeatureFlowContext featureflowContext;

        Evaluate(FeatureFlowClientImpl featureFlowClient, String featureKey, FeatureFlowContext featureFlowContext) {
            this.featureflowClient = featureFlowClient;
            this.featureKey = featureKey;
            this.featureflowContext = featureFlowContext;

        }
        public boolean isOn(){
            return is(Variant.on);
        }
        public boolean isOff(){
            return is(Variant.off);
        }
        public boolean is(String variant){
            String result = featureflowClient.eval(featureKey, featureflowContext);
            return variant.equals(result);
        }
        public String value(){
            String result = featureflowClient.eval(featureKey, featureflowContext);
            return result;
        }
    }
}
