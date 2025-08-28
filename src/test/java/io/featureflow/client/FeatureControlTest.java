package io.featureflow.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.featureflow.client.model.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Updated to use JUnit 5
 */
public class FeatureControlTest {

    public static final String RED = "red";
    public static final String BLUE = "blue";
    public static final String USER_KEY = "userKey";
    public static final String ROLE = "role";
    public static final String TESTER = "tester";
    public static final String END_USER = "endUser";

    @Test
    public void getKey() throws Exception {
        FeatureControl control = new FeatureControl();
        control.key = "FF-01";
        assertEquals("FF-01", control.getKey());
    }


    @Test
    public void evaluate() {

        //create feature and variant
        FeatureControl featureControl = new FeatureControl();
        featureControl.key = "FF-01";
        featureControl.enabled = true;



        //create one rule with an audience
        Rule rule1 = new Rule();
        Audience audience = new Audience(null, null,
                Arrays.asList(
                        new Condition(ROLE, Operator.equals,
                                Arrays.asList(new JsonPrimitive(TESTER)))));
        rule1.setAudience(audience);
        rule1.setVariantSplits(Arrays.asList(new VariantSplit(RED, 0l), new VariantSplit(BLUE, 100l)));

        //create default rule
        Rule rule2 = new Rule();
        rule2.setVariantSplits(Arrays.asList(new VariantSplit(RED, 100l), new VariantSplit(BLUE, 0l)));

        featureControl.rules = Arrays.asList(rule1,rule2);
        FeatureflowUser user = new FeatureflowUser(USER_KEY);

        Map<String, JsonElement> contextValues = new HashMap<>();
        contextValues.put(ROLE, new JsonPrimitive(TESTER));
        user.withAttributes(contextValues);
        String status = featureControl.evaluate(user);
        assertEquals(BLUE, status); //Blue as we are ROLE TESTER

        user = new FeatureflowUser(USER_KEY);
        contextValues = new HashMap<>();
        contextValues.put(ROLE, new JsonPrimitive(END_USER));
        //contextValues.put("age", new JsonPrimitive(26l));
        user.withAttributes(contextValues);

        status = featureControl.evaluate(user);
        assertEquals(status, RED); //Red is the default rule

    }
}