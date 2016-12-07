package io.featureflow.client;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oliver on 25/05/2016.
 * A feature control
 * Variants may be
 *
 * A feature control is set per environment
 * A feature control is matched by evaluating a list of Rules
 * Rules use an Audience to target audiences
 * An audience is a list of Conditions containing Target - Operator - Value
 * The rules then identify a variant split which is a distribution of variation for the audience
 * The VariantSplit uses an equally distributed has function to distribute the required split %ages
 * A seed value is used to generate the split and is set on the feature control. This can be changed to redistribute the splits.
 *
 */
public class FeatureControl {//<V> {
    String key; //the key which is unique per project and used as the human-readable unique key
    String environmentId; //the environmentId
    //String salt; //The salt is used to hash context details (this is in the environment config)  TBC
    int variationsSeed; //The variations seed is a fixed random number that is used in the hashing algorythm

    boolean enabled; //is this feature enabled? If not then we show the offVariant
    boolean available; //is the feature available in the environment?
    boolean deleted; //has this been deleted then ignore in all evaluations
    List<Rule> rules = new ArrayList<>(); //A list of feature rules which contain rules to target variant splits at particular audiences
    int offVariant; // This is served if the feature is toggled off and is the last call but one (the coded in value is the final failover value)
    boolean inClientApi; //is this in the JS api (for any required logic)

    List<Variant> variants = new ArrayList<>();  //available variants for this feature

    public String getKey(){
        return this.key;
    }

    public String evaluate(FeatureFlowContext context) {
        //if off then offVariant
        if(!enabled) return variants.get(offVariant).name;
        //if we have rules (we should always have at least one - the default rule
        for (Rule rule : rules) {
            if(rule.matches(context)){
                //if the rule matches then pass back the variant based on the split evaluation
                return variants.get(rule.getEvaluatedVariant(context.key, variationsSeed)).name;
            }
        }
        return null; //at least the default rule above should have matched, if not, return null to invoke using the failover rule
    }
}
