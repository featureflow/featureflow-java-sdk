package io.featureflow.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.featureflow.client.model.Audience;
import io.featureflow.client.model.Condition;
import io.featureflow.client.model.Operator;
import io.featureflow.client.model.Rule;
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
        c1.values.add(new JsonPrimitive("oliver"));

        Audience a = new Audience();
        a.conditions = Arrays.asList(c1);
        rule.setAudience(a);

        FeatureflowContext context = new FeatureflowContext("oliver");
        Map<String, JsonElement> contextValues = new HashMap<>();
        contextValues.put("name", new JsonPrimitive("oliver"));
        context.values = contextValues;

        Assert.assertTrue(rule.matches(context));

    }

    @Test
    public void testMultipleConditionsMustAllMatch() throws Exception {

        Rule rule = new Rule();

        Condition c1 = new Condition();
        c1.target = "tier";
        c1.operator = Operator.equals;
        c1.values.add(new JsonPrimitive("gold"));

        Condition c2 = new Condition();
        c2.target = "name";
        c2.operator = Operator.equals;
        c2.values.add(new JsonPrimitive("oliver"));

        Audience a = new Audience();
        a.conditions = Arrays.asList(c1, c2);
        rule.setAudience(a);

        FeatureflowContext context = new FeatureflowContext("oliver");
        Map<String, JsonElement> contextValues = new HashMap<>();
        contextValues.put("name", new JsonPrimitive("oliver"));
        contextValues.put("tier", new JsonPrimitive("silver"));
        context.values = contextValues;

        Assert.assertTrue(!rule.matches(context));

    }

    @Test
    public void testRuleGreaterThan() throws Exception {
        Rule rule = new Rule();
        Condition c1 = new Condition();
        c1.target = "age";
        c1.operator = Operator.greaterThan;
        c1.values.add(new JsonPrimitive(25l));

        Audience a = new Audience();
        a.conditions = Arrays.asList(c1);
        rule.setAudience(a);

        FeatureflowContext context = new FeatureflowContext("oliver");
        Map<String, JsonElement> contextValues = new HashMap<>();
        contextValues.put("name", new JsonPrimitive("oliver"));
        contextValues.put("age", new JsonPrimitive(26l));

        context.values = contextValues;

        Assert.assertTrue(rule.matches(context));

    }


}