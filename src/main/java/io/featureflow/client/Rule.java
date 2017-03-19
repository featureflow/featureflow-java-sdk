package io.featureflow.client;

import org.apache.commons.codec.digest.DigestUtils;

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

    public String getVariantSplitKey(String contextKey, String featureKey, String salt){
      //  if(variant!=null)return variant;
        if(contextKey==null)contextKey="anonymous";
        long variantValue = getVariantValue(getHash(contextKey, featureKey, salt));
        return getSplitKey(variantValue);
    }

    public String getSplitKey(long variantValue){
        int percent = 0;
        for (VariantSplit variantSplit : variantSplits) {
            percent += variantSplit.getSplit();
            if(percent >= variantValue)return variantSplit.getVariantKey();
        }
        return null;
    }
    /**
     * Generate the Variant value by
     * @param contextKey - the contexts unique identifier key
     * @param featureKey - The feature key we are testing
     * @param salt - A salt value
     * @return hash - the hashed value
     */
    public String getHash(String contextKey, String featureKey, String salt){
        String hash = DigestUtils.sha1Hex(salt + ":" + featureKey + ":" + contextKey).substring(0, 15);
        return hash;
    }

    public long getVariantValue(String hash) {
        long longVal = Long.parseLong(hash, 16);
        return (longVal % 100) + 1;
    }
}
