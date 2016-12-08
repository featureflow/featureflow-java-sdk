package io.featureflow.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oliver on 23/05/2016.
 */
public class FeatureFlowContext{

    String key;
    Map<String, ? extends JsonElement> values = new HashMap<>();

    public FeatureFlowContext(String key) {
        this.key = key;
    }

    public static Builder keyedContext(String key){
        return new Builder(key);
    }
    public static Builder context(){
        return new Builder();
    }

    public static class Builder{
        private String key;
        private Map<String, JsonElement> values = new HashMap<>();

        public Builder() {}
        public Builder(String key) {
            this.key = key;
        }

        public Builder withValue(String key, JsonElement value){
            this.values.put(key, value);
            return this;
        }
        public FeatureFlowContext build(){
            if(key==null)key = "anonymous";
            FeatureFlowContext context = new FeatureFlowContext(key);
            context.values = values;
            return context;
        }
    }
}
