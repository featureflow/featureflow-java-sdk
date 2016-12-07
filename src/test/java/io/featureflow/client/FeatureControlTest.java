package io.featureflow.client;

import com.google.gson.JsonPrimitive;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by oliver on 15/08/2016.
 */
public class FeatureControlTest {
    @Test
    public void getKey() throws Exception {
        FeatureControl control = new FeatureControl();
        control.key = "FF-01";

        assertTrue(control.getKey().equals("FF-01"));
    }


   /* @Test
    public void evaluate() throws Exception {


        FeatureControl featureControl = new FeatureControl();
        featureControl.key = "FF-01";
        featureControl.enabled = true;

        Variant variant1 = new Variant();
        variant1.value = "red";
        VariantRule rule1 = new VariantRule();
        rule1.target = "role";
        rule1.values = Arrays.asList(new JsonPrimitive("admin"), new JsonPrimitive("manager"));
        rule1.operator = Operator.EQUALS;
        variant1.variantRules.add(rule1);

        Variant variant2 = new Variant();
        variant2.value = "blue";
        VariantRule rule2 = new VariantRule();

        //variant2.variantRule = new VariantRule();
        rule2.target = "role";
        rule2.values = Arrays.asList(new JsonPrimitive("user"), new JsonPrimitive("freemium"));
        rule2.operator = Operator.EQUALS;
        variant2.variantRules.add(rule2);
        featureControl.variants = Arrays.asList(variant1, variant2);

        FeatureFlowContext context = new FeatureFlowContext("key1");
        context.getValues().put("role", new JsonPrimitive("admin"));
        FeatureStatus status = featureControl.evaluate(context, false, "black");
        assertTrue(status.isEnabled()==true);
        assertTrue(status.evaluate().equals("red"));

        //Check non matching variants return defaults
        context =  new FeatureFlowContext("key1");
        context.getValues().put("role", new JsonPrimitive("anonymous"));
        status = featureControl.evaluate(context, false, "black");
        assertTrue(status.isEnabled()==false);
        assertTrue(status.evaluate().equals("black"));

        context =  new FeatureFlowContext("key1");
        context.getValues().put("role", new JsonPrimitive("anonymous"));
        status = featureControl.evaluate(context, false, "black");
        assertTrue(status.isEnabled()==false);
        assertTrue(status.evaluate().equals("black"));



    }
    @Test
    public void testIsWithinRollout(){
        FeatureControl featureControl = new FeatureControl();
        featureControl.key = "FF-01";
        featureControl.enabled = true;
        featureControl.rolloutPercent = 25l;

        FeatureFlowContext context =  new FeatureFlowContext("1");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertFalse(featureControl.isWithinRollout(context));

        context =  new FeatureFlowContext("2");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertTrue(featureControl.isWithinRollout(context));

        context =  new FeatureFlowContext("3");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertFalse(featureControl.isWithinRollout(context));

        context =  new FeatureFlowContext("4");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertFalse(featureControl.isWithinRollout(context));

        context =  new FeatureFlowContext("5");
        context.getValues().put("role", new JsonPrimitive("admin"));
        assertFalse(featureControl.isWithinRollout(context));
    }*/
}