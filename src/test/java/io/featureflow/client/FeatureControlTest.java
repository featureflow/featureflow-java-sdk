package io.featureflow.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.featureflow.client.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by oliver on 15/08/2016.
 */
public class FeatureControlTest {

    public static final String RED = "red";
    public static final String BLUE = "blue";
    public static final String JOHN = "john";
    public static final String NAME = "name";
    public static final String USER_KEY = "userKey";
    public static final String ROLE = "role";
    public static final String TESTER = "tester";
    public static final String END_USER = "endUser";
    public static final String TESTER1 = "tester";

    @Test
    public void getKey() throws Exception {
        FeatureControl control = new FeatureControl();
        control.key = "FF-01";

        assertTrue(control.getKey().equals("FF-01"));
    }


    @Test
    public void evaluate() throws Exception {

        //create feature and variant
        FeatureControl featureControl = new FeatureControl();
        featureControl.key = "FF-01";
        featureControl.enabled = true;

        Variant red = new Variant(RED, RED);
        Variant blue = new Variant(BLUE, BLUE);
        featureControl.variants = Arrays.asList(red,blue);


        //create one rule with an audience
        Rule rule1 = new Rule();
        Audience audience = new Audience(null, null,
                Arrays.asList(
                        new Condition(ROLE, Operator.equals,
                                Arrays.asList(new JsonPrimitive(TESTER)))));
        rule1.setAudience(audience);
        rule1.setPriority(2);
        rule1.setVariantSplits(Arrays.asList(new VariantSplit(RED, 0l), new VariantSplit(BLUE, 100l)));

        //create default rule
        Rule rule2 = new Rule();
        rule2.setPriority(1);
        rule2.setVariantSplits(Arrays.asList(new VariantSplit(RED, 100l), new VariantSplit(BLUE, 0l)));

        featureControl.rules = Arrays.asList(rule1,rule2);
        FeatureflowContext context = new FeatureflowContext(USER_KEY);

        Map<String, JsonElement> contextValues = new HashMap<>();
        contextValues.put(ROLE, new JsonPrimitive(TESTER));
        context.values =contextValues;
        String status = featureControl.evaluate(context);
        assertEquals(BLUE, status); //Blue as we are ROLE TSETER

        context = new FeatureflowContext(USER_KEY);
        contextValues = new HashMap<>();
        contextValues.put(ROLE, new JsonPrimitive(END_USER));
        //contextValues.put("age", new JsonPrimitive(26l));
        context.values =contextValues;

        status = featureControl.evaluate(context);
        assertTrue(status.equals(RED)); //Red sa default rule
/*

        assertTrue(status()==true);
        assertTrue(status.evaluate().equals("red"));

        //Check non matching variants return defaults
        context =  new FeatureflowContext("key1");
        context.getValues().put("role", new JsonPrimitive("anonymous"));
        status = featureControl.evaluate(context, false, "black");
        assertTrue(status.isEnabled()==false);
        assertTrue(status.evaluate().equals("black"));

        context =  new FeatureflowContext("key1");
        context.getValues().put("role", new JsonPrimitive("anonymous"));
        status = featureControl.evaluate(context, false, "black");
        assertTrue(status.isEnabled()==false);
        assertTrue(status.evaluate().equals("black"));*/



    }
  /*  @Test
    public void testIsWithinRollout(){
        FeatureControl featureControl = new FeatureControl();
        featureControl.key = "FF-01";
        featureControl.enabled = true;
        featureControl.rolloutPercent = 25l;

        FeatureflowContext context =  new FeatureflowContext("1");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertFalse(featureControl.isWithinRollout(context));

        context =  new FeatureflowContext("2");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertTrue(featureControl.isWithinRollout(context));

        context =  new FeatureflowContext("3");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertFalse(featureControl.isWithinRollout(context));

        context =  new FeatureflowContext("4");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertFalse(featureControl.isWithinRollout(context));

        context =  new FeatureflowContext("5");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertFalse(featureControl.isWithinRollout(context));
    }*/
}