package io.featureflow.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.featureflow.client.model.Audience;
import io.featureflow.client.model.Condition;
import io.featureflow.client.model.Operator;
import io.featureflow.client.model.Rule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Updated to use JUnit 5
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

        FeatureflowUser user = new FeatureflowUser("oliver");
        Map<String, JsonElement> contextValues = new HashMap<>();
        contextValues.put("name", new JsonPrimitive("oliver"));
        user.withAttributes(contextValues);

        assertTrue(rule.matches(user));

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

        FeatureflowUser user = new FeatureflowUser("oliver");
        Map<String, JsonElement> contextValues = new HashMap<>();
        contextValues.put("name", new JsonPrimitive("oliver"));
        contextValues.put("tier", new JsonPrimitive("silver"));
        user.withAttributes(contextValues);

        assertTrue(!rule.matches(user));

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

        FeatureflowUser user = new FeatureflowUser("oliver");
        Map<String, JsonElement> contextValues = new HashMap<>();
        contextValues.put("name", new JsonPrimitive("oliver"));
        contextValues.put("age", new JsonPrimitive(26l));

        user.withAttributes(contextValues);

        assertTrue(rule.matches(user));

    }

    @Test
    public void testRuleMatchesWithNullAudience() throws Exception {
        Rule rule = new Rule();
        Condition c1 = new Condition();
        c1.target = "age";
        c1.operator = Operator.greaterThan;
        c1.values.add(new JsonPrimitive(25l));

        FeatureflowUser user = new FeatureflowUser("oliver");
        Map<String, JsonElement> contextValues = new HashMap<>();
        contextValues.put("name", new JsonPrimitive("oliver"));
        contextValues.put("age", new JsonPrimitive(26l));

        user.withAttributes(contextValues);

        assertTrue(rule.matches(user));

    }


}