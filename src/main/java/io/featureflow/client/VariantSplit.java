package io.featureflow.client;

/**
 * Created by oliver on 18/11/16.
 */
public class VariantSplit {

    private String variantId; //the variant id
    private Long split;//the split value

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public Long getSplit() {
        return split;
    }

    public void setSplit(Long split) {
        this.split = split;
    }
}
