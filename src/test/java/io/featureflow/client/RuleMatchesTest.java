package io.featureflow.client;

import com.google.gson.JsonPrimitive;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oliver on 21/11/16.
 */
public class RuleMatchesTest {

    @Test
    public void testRuleEquals() throws Exception {

        Rule rule = new Rule();

        Condition c1 = new Condition();
        c1.target = "name";
        c1.operator = Operator.equals;
        c1.value = new JsonPrimitive("oliver");

        Audience a = new Audience();
        a.conditions = Arrays.asList(c1);
        rule.setAudience(a);

        FeatureFlowContext context = new FeatureFlowContext("oliver");
        Map<String, JsonPrimitive> contextValues = new HashMap<>();
        contextValues.put("name", new JsonPrimitive("oliver"));
        context.values = contextValues;

        Assert.assertTrue(rule.matches(context));

    }

    @Test
    public void testRuleGreaterThan() throws Exception {

        Rule rule = new Rule();

        Condition c1 = new Condition();
        c1.target = "age";
        c1.operator = Operator.greaterThan;
        c1.value= new JsonPrimitive(25l);

        Audience a = new Audience();
        a.conditions = Arrays.asList(c1);
        rule.setAudience(a);

        FeatureFlowContext context = new FeatureFlowContext("oliver");
        Map<String, JsonPrimitive> contextValues = new HashMap<>();
        contextValues.put("name", new JsonPrimitive("oliver"));
        contextValues.put("age", new JsonPrimitive(26l));

        context.values = contextValues;

        Assert.assertTrue(rule.matches(context));

    }


}