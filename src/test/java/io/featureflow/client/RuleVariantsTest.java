package io.featureflow.client;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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
        onSplit.setVariantKey(onId);
        onSplit.setSplit(100l);

        VariantSplit offSplit = new VariantSplit();
        offSplit.setVariantKey(offId);
        offSplit.setSplit(0l);


        rule.setVariantSplits(Arrays.asList(onSplit, offSplit));

        Assert.assertEquals(onId, rule.getEvaluatedVariantKey("oliver", "f1", 1));
    }

    @Test
    public void testDefaultOffVariant() throws Exception {

        Rule rule = new Rule();

        String onId = "onId";
        String offId = "ioffId";

        VariantSplit onSplit = new VariantSplit();
        onSplit.setVariantKey(onId);
        onSplit.setSplit(0l);

        VariantSplit offSplit = new VariantSplit();
        offSplit.setVariantKey(offId);
        offSplit.setSplit(100l);


        rule.setVariantSplits(Arrays.asList(onSplit, offSplit));

        Assert.assertEquals(offId, rule.getEvaluatedVariantKey("oliver", "f1", 1));
    }

    @Test
    public void testMultiVariant() throws Exception {


        Rule rule = new Rule();

        String id1 = "id1";
        String id2 = "id2";
        String id3 = "id3";


        VariantSplit redSplit = new VariantSplit();
        redSplit.setVariantKey(id1);
        redSplit.setSplit(10l);

        VariantSplit blueSplit = new VariantSplit();
        blueSplit.setVariantKey(id2);
        blueSplit.setSplit(60l);

        VariantSplit greenSplit = new VariantSplit();
        greenSplit.setVariantKey(id3);
        greenSplit.setSplit(30l);

        rule.setVariantSplits(Arrays.asList(redSplit, blueSplit, greenSplit));

        Assert.assertEquals(id3, rule.getEvaluatedVariantKey("oliver", "f1", 1));

        Assert.assertEquals(id2, rule.getEvaluatedVariantKey("alan", "f1",1));

        Assert.assertEquals(id1, rule.getEvaluatedVariantKey("sarah", "f1",1));
    }



    @Test
    public void testGetVariantValue(){
        List<String> values = Arrays.asList(
                "alice",
                "bob",
                "charlie",
                "daniel",
                "emma",
                "frank",
                "george");

        int[] seeds = {1,2,3};

        for (int seed : seeds) {
            System.out.println("SEED is " + seed);
            for (String value : values) {
                Rule rule = new Rule();
                System.out.println(value + " equals " + rule.getVariantValue(rule.getHash(value, "f1", seed)));
            }
        }

    }




}