package io.featureflow.client.cucumber.stepdefs;

import com.google.gson.JsonPrimitive;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.featureflow.client.model.Operator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Step definitions for the shared testbed's conditions.feature.
 *
 * These scenarios were previously not run here at all: pom.xml's testResource include list
 * omitted conditions.feature, so operator coverage in this SDK came only from hand-written
 * JUnit tests (OperatorTest, OperatorDateTest). Those tests are still worth having, but they
 * can drift from the cross-SDK contract without anything noticing - which is precisely what
 * the shared testbed exists to prevent. Adding the file to the include list requires these
 * steps to exist, because the suite runs strict.
 *
 * Values arrive from Gherkin as strings and are converted according to the type named in the
 * step, mirroring the equivalent step definitions in the Go, Python and Ruby SDKs so that all
 * four feed the operators the same shapes.
 */
public class ConditionsStepDefs {

    private JsonPrimitive target;
    private List<JsonPrimitive> values;
    private boolean output;

    private static JsonPrimitive primitive(String type, String value) {
        if ("number".equals(type)) {
            return new JsonPrimitive(Double.parseDouble(value));
        }
        if ("boolean".equals(type)) {
            return new JsonPrimitive(Boolean.parseBoolean(value));
        }
        return new JsonPrimitive(value);
    }

    @Given("the target is a {string} with the value of {string}")
    public void the_target_is_a_with_the_value_of(String type, String value) {
        this.target = primitive(type, value);
    }

    @Given("the attribute is a {string} with the value of {string}")
    public void the_attribute_is_a_with_the_value_of(String type, String value) {
        this.values = new ArrayList<>();
        this.values.add(primitive(type, value));
    }

    @Given("the attribute is an array of values {string}")
    public void the_attribute_is_an_array_of_values(String csv) {
        this.values = new ArrayList<>();
        for (String value : csv.split(", ")) {
            this.values.add(new JsonPrimitive(value));
        }
    }

    /**
     * An operator name the server may add after this SDK ships must fail closed rather than
     * throw, so an unresolvable name is evaluated as no-match rather than propagating the
     * IllegalArgumentException from Enum.valueOf.
     */
    @When("the operator test {string} is run")
    public void the_operator_test_is_run(String operatorName) {
        Operator operator;
        try {
            operator = Operator.valueOf(operatorName);
        } catch (IllegalArgumentException unknownOperator) {
            this.output = false;
            return;
        }
        this.output = operator.evaluate(this.target, this.values);
    }

    @Then("the output should equal {string}")
    public void the_output_should_equal(String expected) {
        assertEquals(Boolean.parseBoolean(expected), this.output,
                "operator result for target " + this.target + " against " + this.values);
    }
}
