package io.featureflow.client.model;

import io.featureflow.client.FeatureflowUser;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;

/**
 * A Rule holds and Audience (Who to show to) and a list of VariantSplits (what to show them)
 */
public class Rule {

    public static final String ANONYMOUS = "anonymous";
    private int priority;
    private Audience audience;
    private List<VariantSplit> variantSplits; //user may split the variant between users

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
    public boolean matches(FeatureflowUser user){
        return audience==null || audience.matches(user);
    }

    public String getVariantSplitKey(String contextKey, String featureKey, String salt){
        if(contextKey==null)contextKey= ANONYMOUS;
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
     * Generate the Variant value using sha1hex
     * 1. We generate an equally distributed string of hex values, parse it to a length of 15,
     *      thats the max we can get before we blow out of the long range (fffffffffffffff)16 = (1152921504606846975)10
     * 2. We turn that hex into its representative number
     * 3. We find the remainder from 100 and use that as our variant bucket
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
