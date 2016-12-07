package io.featureflow.client;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by oliver on 21/11/16.
 */
public class RuleVariantsTest {


    @Test
    public void testDefaultOnVariant() throws Exception {

        Rule rule = new Rule();

        VariantSplit onSplit = new VariantSplit();
        onSplit.setVariant(0);
        onSplit.setSplit(100l);

        VariantSplit offSplit = new VariantSplit();
        offSplit.setVariant(1);
        offSplit.setSplit(0l);


        rule.setVariantSplits(Arrays.asList(onSplit, offSplit));

        Assert.assertEquals(0, rule.getEvaluatedVariant("oliver", 1));
    }

    @Test
    public void testDefaultOffVariant() throws Exception {

        Rule rule = new Rule();

        VariantSplit onSplit = new VariantSplit();
        onSplit.setVariant(0);
        onSplit.setSplit(0l);

        VariantSplit offSplit = new VariantSplit();
        offSplit.setVariant(1);
        offSplit.setSplit(100l);


        rule.setVariantSplits(Arrays.asList(onSplit, offSplit));

        Assert.assertEquals(1, rule.getEvaluatedVariant("oliver", 1));
    }

    @Test
    public void testMultiVariant() throws Exception {

        Rule rule = new Rule();

        VariantSplit redSplit = new VariantSplit();
        redSplit.setVariant(0);
        redSplit.setSplit(10l);

        VariantSplit blueSplit = new VariantSplit();
        blueSplit.setVariant(1);
        blueSplit.setSplit(60l);

        VariantSplit greenSplit = new VariantSplit();
        greenSplit.setVariant(2);
        greenSplit.setSplit(30l);

        rule.setVariantSplits(Arrays.asList(redSplit, blueSplit, greenSplit));

        Assert.assertEquals(2, rule.getEvaluatedVariant("oliver", 1));

        Assert.assertEquals(1, rule.getEvaluatedVariant("alan", 1));

        Assert.assertEquals(0, rule.getEvaluatedVariant("sarah", 1));
    }




}