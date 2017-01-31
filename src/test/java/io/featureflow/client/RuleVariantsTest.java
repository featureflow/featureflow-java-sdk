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

        String onId = "onId";
        String offId = "ioffId";
        VariantSplit onSplit = new VariantSplit();
        onSplit.setVariantId(onId);
        onSplit.setSplit(100l);

        VariantSplit offSplit = new VariantSplit();
        offSplit.setVariantId(offId);
        offSplit.setSplit(0l);


        rule.setVariantSplits(Arrays.asList(onSplit, offSplit));

        Assert.assertEquals(onId, rule.getEvaluatedVariantId("oliver", 1));
    }

    @Test
    public void testDefaultOffVariant() throws Exception {

        Rule rule = new Rule();

        String onId = "onId";
        String offId = "ioffId";

        VariantSplit onSplit = new VariantSplit();
        onSplit.setVariantId(onId);
        onSplit.setSplit(0l);

        VariantSplit offSplit = new VariantSplit();
        offSplit.setVariantId(offId);
        offSplit.setSplit(100l);


        rule.setVariantSplits(Arrays.asList(onSplit, offSplit));

        Assert.assertEquals(offId, rule.getEvaluatedVariantId("oliver", 1));
    }

    @Test
    public void testMultiVariant() throws Exception {


        Rule rule = new Rule();

        String id1 = "id1";
        String id2 = "id2";
        String id3 = "id3";


        VariantSplit redSplit = new VariantSplit();
        redSplit.setVariantId(id1);
        redSplit.setSplit(10l);

        VariantSplit blueSplit = new VariantSplit();
        blueSplit.setVariantId(id2);
        blueSplit.setSplit(60l);

        VariantSplit greenSplit = new VariantSplit();
        greenSplit.setVariantId(id3);
        greenSplit.setSplit(30l);

        rule.setVariantSplits(Arrays.asList(redSplit, blueSplit, greenSplit));

        Assert.assertEquals(id3, rule.getEvaluatedVariantId("oliver", 1));

        Assert.assertEquals(id2, rule.getEvaluatedVariantId("alan", 1));

        Assert.assertEquals(id1, rule.getEvaluatedVariantId("sarah", 1));
    }




}