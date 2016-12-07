package io.featureflow.client;

/**
 * Created by oliver on 18/11/16.
 */
public class VariantSplit {

    private int variant; //the variant index
    private Long split;//the split value

    public int getVariant() {
        return variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
    }

    public Long getSplit() {
        return split;
    }

    public void setSplit(Long split) {
        this.split = split;
    }
}
