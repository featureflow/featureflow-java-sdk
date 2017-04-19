package io.featureflow.client.cucumber.stepdefs;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import gherkin.formatter.model.DataTableRow;
import io.featureflow.client.*;
import io.featureflow.client.model.*;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by oliver.oldfieldhodge on 9/3/17.
 */
public class RulesStepDefs {
    Rule rule = new Rule();
    FeatureFlowContext context;
    boolean ruleToContextMatch;
    List<Condition> conditions;
    Audience audience;
    int variantSplitValue;
    String variantSplitKey;
    List<VariantSplit> variantSplits;

    Gson gson = new Gson();
    JsonParser parser = new JsonParser();

    @Given("^the rule is a default rule$")
    public void the_rule_is_a_default_rule() throws Throwable {
        rule = new Rule();
    }

    @When("^the rule is matched against the context$")
    public void the_rule_is_matched_against_the_context() throws Throwable {
        ruleToContextMatch = TestAccessor.matches(rule, context);
    }


    @Then("^the result from the match should be true$")
    public void the_result_from_the_match_should_be_true() throws Throwable {
        assertTrue(ruleToContextMatch);
    }

    @Given("^the context values are$")
    public void the_context_values_are(DataTable contextValues) throws Throwable {
        context = new FeatureFlowContext("uniquecontextkey");
        Map<String, JsonElement> contextVals = new HashMap<>();
        for (DataTableRow dataTableRow : contextValues.getGherkinRows()) {
            if("key".equals(dataTableRow.getCells().get(0)))continue;
            //JsonElement lement = gson.fromJson(dataTableRow.getCells().get(1));
            //we need to convert to an array or a primitive
            JsonElement contextVal;
            if(dataTableRow.getCells().get(1).startsWith("[")) {

                JsonElement el = parser.parse(dataTableRow.getCells().get(1));
                JsonArray arr = el.getAsJsonArray();
                contextVal = arr;
            }else{
                contextVal = parser.parse(dataTableRow.getCells().get(1)).getAsJsonPrimitive();
            }

            contextVals.put(dataTableRow.getCells().get(0), contextVal);
        }
        TestAccessor.setContextValues(context, contextVals);
    }

    @Given("^the rule's audience conditions are$")
    public void the_rule_s_audience_conditions_are(DataTable audienceValues) throws Throwable {
        conditions = new ArrayList<>();
        rule = new Rule();
        for (DataTableRow dataTableRow : audienceValues.getGherkinRows()) {
            if("operator".equals(dataTableRow.getCells().get(0)))continue;
            Operator operator = Operator.valueOf(dataTableRow.getCells().get(0)); //get operator
            String target = dataTableRow.getCells().get(1);
            List<JsonPrimitive> values = gson.fromJson(dataTableRow.getCells().get(2), new TypeToken<List<JsonPrimitive>>(){}.getType()); //get values array
            Condition condition = new Condition(target, operator, values);
            conditions.add(condition);
        }
        audience = new Audience("audience1", "Audience One", conditions);
        rule.setAudience(audience);

    }

    @Then("^the result from the match should be false$")
    public void the_result_from_the_match_should_be_false() throws Throwable {
        assertFalse(audience.matches(context));
    }

    @Given("^the variant value of (\\d+)$")
    public void the_variant_value_of(int variantSplitValue) throws Throwable {
        this.variantSplitValue = variantSplitValue;
    }

    @Given("^the variant splits are$")
    public void the_variant_splits_are(DataTable variantSplitTable) throws Throwable {
        variantSplits = new ArrayList<>();
        for (DataTableRow dataTableRow : variantSplitTable.getGherkinRows()) {
            if(dataTableRow.getCells().get(0).equals("variantKey"))continue;
            variantSplits.add(new VariantSplit(dataTableRow.getCells().get(0), new Long(dataTableRow.getCells().get(1))));
        }
        rule.setVariantSplits(variantSplits);
    }

    @When("^the variant split key is calculated$")
    public void the_variant_split_key_is_calculated() throws Throwable {
        variantSplitKey = rule.getSplitKey(variantSplitValue);

    }

    @Then("^the resulting variant should be \"([^\"]*)\"$")
    public void the_resulting_variant_should_be(String variantResult) throws Throwable {
        Assert.assertEquals(variantResult, variantSplitKey);
    }


}
