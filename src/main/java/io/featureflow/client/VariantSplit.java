package io.featureflow.client;

/**
 * Created by oliver on 18/11/16.
 */
public class VariantSplit {

    private String variantKey; //the variant id
    private Long split;//the split value

    public String getVariantKey() {
        return variantKey;
    }

    public void setVariantKey(String variantKey) {
        this.variantKey = variantKey;
    }

    public Long getSplit() {
        return split;
    }

    public void setSplit(Long split) {
        this.split = split;
    }
}
