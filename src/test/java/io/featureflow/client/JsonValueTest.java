package io.featureflow.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.featureflow.client.core.EventsClientImpl;
import io.featureflow.client.model.FeatureControl;
import io.featureflow.client.model.Rule;
import io.featureflow.client.model.Variant;
import io.featureflow.client.model.VariantSplit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the same ground as the testbed's json_value.feature (tagged @json-value,
 * JS-family-only per its own header — not part of the required cross-SDK contract, and
 * its final scenario assumes a raw per-evaluation event shape this SDK doesn't have,
 * since evaluate events are summarised here rather than sent one-per-evaluation. Plain
 * JUnit instead of Cucumber, matching the precedent set by OperatorTest/RuleMatchesTest
 * for conditions/userBuilder-equivalent coverage.
 */
public class JsonValueTest {

    private static final String FEATURE_KEY = "my-feature";

    private FeatureflowClient clientEvaluatingTo(String evaluatedVariant, List<Variant> variants) {
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withFeatureEventUri("http://127.0.0.1:9")
                .withPollingInterval(0)
                .withWaitForStartup(0)
                .build();
        // A short (<=10 char) apiKey stops the polling client fetching, and interval 0
        // disables polling and lazy refresh, so the client runs entirely offline — the
        // stored feature control is injected directly into its cache below. Matches the
        // pattern in EventsStepDefs' "a Featureflow client with the stored features".
        FeatureflowClient client = FeatureflowClient.builder("test-key").withConfig(config).build();

        FeatureControl control = new FeatureControl();
        control.key = FEATURE_KEY;
        control.enabled = true;
        control.offVariantKey = Variant.off;
        Rule rule = new Rule();
        rule.setVariantSplits(List.of(new VariantSplit(evaluatedVariant, 100L)));
        control.rules = new ArrayList<>(List.of(rule));
        control.variants = variants;

        Map<String, FeatureControl> controls = new HashMap<>();
        controls.put(FEATURE_KEY, control);
        TestAccessor.setFeatureControls(client, controls);
        return client;
    }

    @Test
    public void jsonValueReturnsTheConfigValueForTheEvaluatedVariant() {
        Variant on = new Variant(Variant.on, "On");
        on.value = JsonParser.parseString("{\"color\":\"#0066cc\",\"maxItems\":10}");
        FeatureflowClient client = clientEvaluatingTo(Variant.on, List.of(on, new Variant(Variant.off, "Off")));

        JsonElement result = client.evaluate(FEATURE_KEY, "user-1").jsonValue();

        assertEquals(JsonParser.parseString("{\"color\":\"#0066cc\",\"maxItems\":10}"), result);
    }

    @Test
    public void jsonValueReturnsNullWhenTheEvaluatedVariantHasNoValue() {
        FeatureflowClient client = clientEvaluatingTo(Variant.off, List.of(new Variant(Variant.on, "On"), new Variant(Variant.off, "Off")));

        assertNull(client.evaluate(FEATURE_KEY, "user-1").jsonValue());
    }

    @Test
    public void jsonValueReturnsNullWhenTheFeatureHasNoVariantsAtAll() {
        FeatureflowClient client = clientEvaluatingTo(Variant.on, new ArrayList<>());

        assertNull(client.evaluate(FEATURE_KEY, "user-1").jsonValue());
    }

    @Test
    public void jsonValueFallsBackToTheSuppliedDefault() {
        FeatureflowClient client = clientEvaluatingTo(Variant.off, List.of(new Variant(Variant.on, "On"), new Variant(Variant.off, "Off")));
        JsonElement fallback = JsonParser.parseString("{\"maxItems\":1}");

        assertEquals(fallback, client.evaluate(FEATURE_KEY, "user-1").jsonValue(fallback));
    }

    @Test
    public void valueStillReturnsTheVariantKeyStringUnaffectedByJsonValue() {
        Variant on = new Variant(Variant.on, "On");
        on.value = JsonParser.parseString("{\"color\":\"#0066cc\",\"maxItems\":10}");
        FeatureflowClient client = clientEvaluatingTo(Variant.on, List.of(on, new Variant(Variant.off, "Off")));

        assertEquals(Variant.on, client.evaluate(FEATURE_KEY, "user-1").value());
    }

    @Test
    public void jsonValueRecordsAnEvaluateEventJustLikeValue() {
        Variant on = new Variant(Variant.on, "On");
        on.value = JsonParser.parseString("{\"color\":\"#0066cc\",\"maxItems\":10}");
        FeatureflowClient client = clientEvaluatingTo(Variant.on, List.of(on, new Variant(Variant.off, "Off")));
        EventsClientImpl eventsClient = (EventsClientImpl) TestAccessor.getEventHandler(client);

        client.evaluate(FEATURE_KEY, "user-1").jsonValue();

        assertEquals(1, eventsClient.getPendingSummaryCount());
        EventsClientImpl.SummarySnapshot summary = eventsClient.getSummarySnapshot().get(0);
        assertEquals(FEATURE_KEY, summary.featureKey);
        assertEquals(Variant.on, summary.evaluatedVariant);
    }
}
