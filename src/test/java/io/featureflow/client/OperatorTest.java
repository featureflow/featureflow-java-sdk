package io.featureflow.client;

import com.google.gson.JsonPrimitive;
import io.featureflow.client.model.Operator;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by oliver on 21/11/16.
 */
public class OperatorTest {
    @Test
    public void evaluateOperators() throws Exception {
        assertTrue(Operator.after.evaluate(new JsonPrimitive("2016-11-20T20:36:19Z"), Arrays.asList(new JsonPrimitive("2016-11-20T20:36:18Z"))));
        assertTrue(Operator.before.evaluate(new JsonPrimitive("2016-11-20T20:36:18Z"), Arrays.asList(new JsonPrimitive("2016-11-20T20:36:19Z"))));
        assertTrue(Operator.contains.evaluate(new JsonPrimitive("oliver"), Arrays.asList(new JsonPrimitive("liver"))));
        assertTrue(Operator.endsWith.evaluate(new JsonPrimitive("oliver"), Arrays.asList(new JsonPrimitive("liver"))));
        assertTrue(Operator.startsWith.evaluate(new JsonPrimitive("oliver"), Arrays.asList(new JsonPrimitive("oli"))));
        assertTrue(Operator.contains.evaluate(new JsonPrimitive("oliver"), Arrays.asList(new JsonPrimitive("live"))));
        assertTrue(Operator.equals.evaluate(new JsonPrimitive("oliver"), Arrays.asList(new JsonPrimitive("oliver"))));
        assertTrue(Operator.greaterThan.evaluate(new JsonPrimitive(1d), Arrays.asList(new JsonPrimitive(0.999d))));
        assertTrue(Operator.greaterThanOrEqual.evaluate(new JsonPrimitive(1d), Arrays.asList(new JsonPrimitive(0.999d))));
        assertTrue(Operator.greaterThanOrEqual.evaluate(new JsonPrimitive(1d), Arrays.asList(new JsonPrimitive(1d))));
        assertTrue(Operator.greaterThan.evaluate(new JsonPrimitive(1d), Arrays.asList(new JsonPrimitive(0.999d))));
        assertTrue(Operator.lessThanOrEqual.evaluate(new JsonPrimitive(0.999d), Arrays.asList(new JsonPrimitive(1d))));
        assertTrue(Operator.lessThanOrEqual.evaluate(new JsonPrimitive(1d), Arrays.asList(new JsonPrimitive(1d))));
        assertTrue(Operator.lessThanOrEqual.evaluate(new JsonPrimitive(1d), Arrays.asList(new JsonPrimitive(1d))));
        assertTrue(Operator.matches.evaluate(new JsonPrimitive("oliver@featureflow.io"), Arrays.asList(new JsonPrimitive( "^(oliver).*$"))));

    }

}