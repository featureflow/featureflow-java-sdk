package io.featureflow.client;

import java.util.List;

/**
 * Created by oliver on 15/08/2016.
 */
public class FeatureRegistration {
    final List<Variant> variants;
    final String key;


    public FeatureRegistration(String key, List<Variant> variants) {
        this.key = key;
        this.variants = variants;
    }
    public FeatureRegistration(String key) {
        this.key = key;
        this.variants = null; //if null then we check for existing variants in config or default to on/off
    }


    class Builder {
        private String key;
        private List<Variant> variants;
        public Builder (String key){
            this.key= key;
        }
        public Builder withVariant(Variant variant){
            variants.add(variant);
            return this;
        }
        public FeatureRegistration build(){
            return new FeatureRegistration(key, variants);
        }
    }

}
