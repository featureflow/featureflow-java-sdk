package io.featureflow.client;

import io.featureflow.client.core.CallbackEvent;
import io.featureflow.client.model.Feature;
import io.featureflow.client.model.Variant;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oliver on 15/08/2016.
 */
public interface FeatureflowClient<E extends Enum<E>> extends Closeable {



    Evaluate evaluate(String featureKey, FeatureflowContext featureflowContext);
    Evaluate evaluate(String featureKey);
    Map<String,String> evaluateAll(FeatureflowContext featureflowContext);
    static Builder builder(String apiKey){
        return new Builder(apiKey);
    }

    class Builder {
        private FeatureflowConfig config = null;
        private String apiKey;
        private Map<CallbackEvent, List<FeatureControlCallbackHandler>> featureControlCallbackHandlers = new HashMap<>();
        private List<Feature> features = new ArrayList<>();



        public Builder (String apiKey){
            this.apiKey = apiKey;
        }

        public Builder withUpdateCallback(FeatureControlCallbackHandler featureControlCallbackHandler){
            this.withCallback(CallbackEvent.UPDATED_FEATURE, featureControlCallbackHandler);
            return this;
        }
        public Builder withDeleteCallback(FeatureControlCallbackHandler featureControlCallbackHandler){
            this.withCallback(CallbackEvent.DELETED_FEATURE, featureControlCallbackHandler);
            return this;
        }
        @Deprecated //use withUpdate or withDelete callbacks e.g. .withUpdateCallback(control -> System.out.println(control.getKey()))
        public Builder withCallback(FeatureControlCallbackHandler featureControlCallbackHandler){
            withUpdateCallback(featureControlCallbackHandler);
            return this;
        }
        public Builder withCallback(CallbackEvent event, FeatureControlCallbackHandler featureControlCallbackHandler){
            if(featureControlCallbackHandlers.get(event)==null){
                featureControlCallbackHandlers.put(event, new ArrayList<>());
            }
            this.featureControlCallbackHandlers.get(event).add(featureControlCallbackHandler);
            return this;
        }

        public Builder withConfig(FeatureflowConfig config){
            this.config = config;
            return this;
        }

        public Builder withFeature(Feature feature){
            this.features.add(feature);
            return this;
        }
        public Builder withFeatures(List<Feature> features){
            this.features = features;
            return this;
        }

        public FeatureflowClient build(){
            if(config==null){ config = new FeatureflowConfig.Builder().build();}
            return new FeatureflowClientImpl(apiKey, features, config, featureControlCallbackHandlers);
        }
    }

    class Evaluate {
        private String evaluateResult;

        Evaluate(FeatureflowClientImpl featureflowClient, String featureKey, FeatureflowContext featureflowContext) {
            evaluateResult = featureflowClient.eval(featureKey, featureflowContext);
        }
        public boolean isOn(){
            return is(Variant.on);
        }
        public boolean isOff(){
            return is(Variant.off);
        }
        public boolean is(String variant){
            return variant.equals(evaluateResult);
        }
        public String value(){
            return evaluateResult;
        }
    }
}
