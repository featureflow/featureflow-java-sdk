package io.featureflow.client.cucumber.stepdefs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.featureflow.client.model.Rule;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Updated to use modern Cucumber API
 */
public class HashingAlgorithmStepDefs {
    String salt;
    String featureKey;
    String contextKey;
    String hash;
    private long result;

    @Given("the salt is {string}, the feature is {string} and the key is {string}")
    public void the_salt_is_the_feature_is_and_the_key_is(String salt, String featureKey, String contextKey) {
        this.salt = salt;
        this.featureKey = featureKey;
        this.contextKey = contextKey;
    }

    @When("the variant value is calculated")
    public void the_variant_value_is_calculated() {
        Rule rule = new Rule();
        this.hash = rule.getHash(this.contextKey, this.featureKey, this.salt);
        this.result = rule.getVariantValue(this.hash);
    }

    @Then("the hash value calculated should equal {string}")
    public void the_hash_value_calculated_should_equal(String hashValue) {
        assertEquals(hashValue, this.hash);
    }

    @Then("the result from the variant calculation should be {long}")
    public void the_result_from_the_variant_calculation_should_be(long resultValue) {
        assertEquals(resultValue, this.result);
    }
}
