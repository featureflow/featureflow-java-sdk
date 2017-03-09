package io.featureflow.client.cucumber.stepdefs;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.featureflow.client.Rule;

import static org.junit.Assert.assertEquals;

/**
 * Created by oliver.oldfieldhodge on 9/3/17.
 */
public class HashingAlgorithmStepDefs {
    int salt;
    String featureKey;
    String contextKey;
    String hash;
    private long result;

    @Given("^the salt is \"([^\"]*)\", the feature is \"([^\"]*)\" and the key is \"([^\"]*)\"$")
    public void the_salt_is_the_feature_is_and_the_key_is(String salt, String featureKey, String contextKey) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        this.salt = new Integer(salt);
        this.featureKey = featureKey;
        this.contextKey = contextKey;
    }

    @When("^the variant value is calculated$")
    public void the_variant_value_is_calculated() throws Throwable {
        Rule rule = new Rule();
        this.hash = rule.getHash(this.contextKey, this.featureKey, this.salt);
        this.result = rule.getVariantValue(this.hash);
    }

    @Then("^the hash value calculated should equal \"([^\"]*)\"$")
    public void the_hash_value_calculated_should_equal(String hashValue) throws Throwable {
        assertEquals(hashValue, this.hash);
    }

    @Then("^the result from the variant calculation should be (\\d+)$")
    public void the_result_from_the_variant_calculation_should_be(long resultValue) throws Throwable {
        assertEquals(resultValue, this.result);
    }

}
