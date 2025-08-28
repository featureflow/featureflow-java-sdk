package io.featureflow.client.cucumber.stepdefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.featureflow.client.FeatureflowUser;
import io.featureflow.client.TestAccessor;
import io.featureflow.client.model.Audience;
import io.featureflow.client.model.Condition;
import io.featureflow.client.model.Operator;
import io.featureflow.client.model.Rule;
import io.featureflow.client.model.VariantSplit;

/**
 * Updated to use modern Cucumber API
 */
public class RulesStepDefs {
    Rule rule = new Rule();
    FeatureflowUser user;
    boolean ruleToUserMatch;
    List<Condition> conditions;
    Audience audience;
    int variantSplitValue;
    String variantSplitKey;
    List<VariantSplit> variantSplits;

    Gson gson = new Gson();
    JsonParser parser = new JsonParser();

    @Given("the rule is a default rule")
    public void the_rule_is_a_default_rule() {
        rule = new Rule();
    }

    @When("the rule is matched against the user")
    public void the_rule_is_matched_against_the_user() {
        ruleToUserMatch = TestAccessor.matches(rule, user);
    }

    @Then("the result from the match should be true")
    public void the_result_from_the_match_should_be_true() {

        assertTrue(ruleToUserMatch);
    }

    @Then("the result from the match should be {string}")
    public void the_result_from_the_match_should_be(String expectedResult) {
        boolean expected = Boolean.parseBoolean(expectedResult);
        assertEquals(expected, ruleToUserMatch);
    }

    @Given("the user attributes are")
    public void the_user_values_are(DataTable userAttributes) {
        user = new FeatureflowUser("uniqueuserid");
        Map<String, JsonElement> userAttrs = new HashMap<>();
        List<Map<String, String>> rows = userAttributes.asMaps();

        for (Map<String, String> row : rows) {
            String key = row.get("attribute");
            String value = row.get("value");

            // Skip header if present
            if (key == null || key.equals("attribute")) continue;

            JsonElement userVal;
            if (value.startsWith("[")) {
                JsonElement el = parser.parse(value);
                JsonArray arr = el.getAsJsonArray();
                userVal = arr;
            } else {
                userVal = parser.parse(value).getAsJsonPrimitive();
            }

            userAttrs.put(key, userVal);
        }
        user.withAttributes(userAttrs);
    }

    @Given("the rule's audience conditions are")
    public void the_rule_s_audience_conditions_are(DataTable audienceValues) {
        conditions = new ArrayList<>();
        rule = new Rule();
        List<Map<String, String>> rows = audienceValues.asMaps();

        for (Map<String, String> row : rows) {
            String operatorStr = row.get("operator");
            String target = row.get("target");
            String valuesStr = row.get("values");

            // Skip header if present
            if (operatorStr == null || operatorStr.equals("operator")) continue;

            Operator operator = Operator.valueOf(operatorStr);
            List<JsonPrimitive> values = gson.fromJson(valuesStr, new TypeToken<List<JsonPrimitive>>(){}.getType());
            Condition condition = new Condition(target, operator, values);
            conditions.add(condition);
        }
        audience = new Audience("audience1", "Audience One", conditions);
        rule.setAudience(audience);
    }

    @Then("the result from the match should be false")
    public void the_result_from_the_match_should_be_false() {
        assertFalse(ruleToUserMatch);
    }

    @Given("the variant value of {int}")
    public void the_variant_value_of(int variantSplitValue) {
        this.variantSplitValue = variantSplitValue;
    }

    @Given("the variant splits are")
    public void the_variant_splits_are(DataTable variantSplitTable) {
        variantSplits = new ArrayList<>();
        List<Map<String, String>> rows = variantSplitTable.asMaps();

        for (Map<String, String> row : rows) {
            String variantKey = row.get("variantKey");
            String splitValue = row.get("split");

            // Skip header if present
            if (variantKey == null || variantKey.equals("variantKey")) continue;
            
            // Skip rows with null split values
            if (splitValue == null) continue;

            variantSplits.add(new VariantSplit(variantKey, Long.valueOf(splitValue)));
        }
        rule.setVariantSplits(variantSplits);
    }

    @When("the variant split key is calculated")
    public void the_variant_split_key_is_calculated() {
        variantSplitKey = rule.getSplitKey(variantSplitValue);
    }

    @Then("the resulting variant should be {string}")
    public void the_resulting_variant_should_be(String variantResult) {
        assertEquals(variantResult, variantSplitKey);
    }
}
