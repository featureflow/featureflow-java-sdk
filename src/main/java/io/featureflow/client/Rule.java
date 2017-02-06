package io.featureflow.client;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by oliver on 18/11/16.
 */
public class Rule {

    private int priority; //do we need this? Just keep ordered
    private Audience audience;
    //if the audience has an id then we are referencing a saved audience in the project model,
    // if there is no audience then this is the default Rule (ie all users)
    private List<VariantSplit> variantSplits; //user may split the variant between users
//    private String variant; //or just choose a variant


    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<VariantSplit> getVariantSplits() {
        return variantSplits;
    }

    public void setVariantSplits(List<VariantSplit> variantSplits) {
        this.variantSplits = variantSplits;
    }

    /*public String evaluate() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }
*/
    boolean matches(FeatureFlowContext context){
        return audience==null?true:audience.matches(context);
    }

    public String getEvaluatedVariantKey(String key, int seed){
      //  if(variant!=null)return variant;

        int variantValue = getVariantValue(getHash(key, seed));
        int percent = 0;
        for (VariantSplit variantSplit : variantSplits) {
            percent += variantSplit.getSplit();
            if(percent > variantValue)return variantSplit.getVariantKey();
        }
        return null;
    }
    private int getVariantValue(int hash) {
        return Math.abs(hash % 100) + 1;
    }
    private int getHash(String key, int seed) {
        int h = 0;
        for(int i = 0; i < key.length(); ++i) {
            h = 31 * h + key.charAt(i);
        }
        return h ^ seed;
    }
}
