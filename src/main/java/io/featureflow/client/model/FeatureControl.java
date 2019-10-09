package io.featureflow.client.model;

import io.featureflow.client.FeatureflowUser;

import java.util.ArrayList;
import java.util.List;

/**
 * A feature control hold the configuration for a feature for a given environment. 
 * A feature control is matched by evaluating a List of type Rule
 * A Rule contains an {@link Audience } and a List of io.featureflow.client.model.VariantSplit - for a given audience we apply the given splits
 * A {@link VariantSplit} defines which variant is shown to a proportion of users. If a rule is for one Variant only then the split will be 100% for that variant.
 * An Audience is a list of Conditions containing a Target - Operator - Value* 
 * A seed value is used to generate the split and is set on the feature control. This can be changed to redistribute the splits. 
 */
public class FeatureControl {//<V> {
    /**
     * The internal unique identifier for this feature control
     */
    public String id;
    public String featureId;
    public String key; //the key which is unique per project and used as the human-readable unique key
    public String environmentId; //the environmentId
    public String salt = "1"; //The salt is used to hash context details (this is in the environment config)  TBC

    public boolean enabled; //is this feature enabled? If not then we show the offVariant
    public boolean available; //is the feature available in the environment?
    public boolean deleted; //has this been deleted then ignore in all evaluations
    public List<Rule> rules = new ArrayList<>(); //A list of feature rules which contain rules to target variant splits at particular audiences
    public String offVariantKey; // This is served if the feature is toggled off and is the last call but one (the coded in value is the final failover value)
    public boolean inClientApi; //is this in the JS api (for any required logic)

    public List<Variant> variants = new ArrayList<>();  //available variants for this feature

    public String getKey(){
        return this.key;
    }

    public String evaluate(FeatureflowUser user) {
        //if off then offVariant
        if(!enabled) {
            return offVariantKey;
        }
        //if we have rules (we should always have at least one - the default rule
        for (Rule rule : rules) {
            if(rule.matches(user)){
                //if the rule matches then pass back the variant based on the split evaluation
                //return //getVariantByKey(rule.getVariantSplitKey(context.key, variationsSeed)).key;
                return rule.getVariantSplitKey(user.getBucketKey()==null?user.getId():user.getBucketKey(), this.key, salt);
            }
        }
        return null; //at least the default rule above should have matched, if not, return null to invoke using the failover rule
    }
    //helpers
    public Variant getVariantByKey(String key){
        for (Variant v: variants) {
            if(v.key.equals(key)){
                return v;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        /*String variants;
        for (Variant variant : variants) {
            variants += variant.name + " ";
        }*/
        return "FeatureControl{" + "\n" +
                "  key='" + key + '\'' + "\n" +
                "  environmentId='" + environmentId + '\'' + "\n" +
                "  enabled=" + enabled + "\n" +
                "  available=" + available + "\n" +
                "  deleted=" + deleted + "\n" +
                //insert barely readable one-liner here:
                "  rules=" + rules.stream().map(r -> "Rule: " + r.getVariantSplits().stream().map(s -> s.getVariantKey() + ":" + s.getSplit() +"% ").reduce("", String::concat) + "\n").reduce("", String::concat) + "\n" +
                "  offVariantKey=" + offVariantKey + "\n" +
                "  inClientApi=" + inClientApi + "\n" +
                "  variants=" + (variants==null?null:variants.stream().map(v -> v.name +" ").reduce("", String::concat)) + "\n" +
                '}';
    }

}
