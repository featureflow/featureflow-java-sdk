package io.featureflow.client.cucumber.stepdefs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpServer;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import io.featureflow.client.FeatureflowClient;
import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.FeatureflowUser;
import io.featureflow.client.TestAccessor;
import io.featureflow.client.core.EventsClientImpl;
import io.featureflow.client.core.FeatureflowPollingClient;
import io.featureflow.client.core.RestClient;
import io.featureflow.client.core.RestClientImpl;
import io.featureflow.client.core.SimpleMemoryFeatureCache;
import io.featureflow.client.model.Event;
import io.featureflow.client.model.FeatureControl;
import io.featureflow.client.model.Rule;
import io.featureflow.client.model.VariantSplit;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for events.feature, goals.feature, sdk_config.feature and
 * tracked_exposures.feature — the "events roadmap" contract shared across SDKs,
 * mirroring featureflow-node-sdk's step definitions. See SDK-CONFIG.md.
 */
public class EventsStepDefs {
    private static final Gson GSON = new Gson();
    private static final Type EVENTS_LIST_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();

    private EventsClientImpl eventsClient;
    private FeatureflowClient featureflowClient;
    private FeatureflowPollingClient pollingClient;
    private Map<String, String> evaluatedFeatures;

    private HttpServer eventsServer;
    private HttpServer featuresServer;
    private String eventsUrl;
    private String featuresUrl;
    private List<ReceivedRequest> receivedRequests = new ArrayList<>();
    private int updatedEventCount;

    @After
    public void tearDown() throws IOException {
        if (eventsClient != null) {
            eventsClient.close();
        }
        if (featureflowClient != null) {
            featureflowClient.close();
        }
        if (pollingClient != null) {
            pollingClient.close();
        }
        if (eventsServer != null) {
            eventsServer.stop(0);
        }
        if (featuresServer != null) {
            featuresServer.stop(0);
        }
    }

    // --- Polls until check stops throwing, so steps can wait on the background flush
    // timer / async server-config application without racing it. ---
    private void eventually(Runnable check) {
        long started = System.currentTimeMillis();
        AssertionError lastFailure = null;
        while (System.currentTimeMillis() - started < 2000) {
            try {
                check.run();
                return;
            } catch (AssertionError e) {
                lastFailure = e;
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        check.run();
        if (lastFailure != null) {
            throw lastFailure;
        }
    }

    private static final class ReceivedRequest {
        final String method;
        final List<Map<String, Object>> events;

        ReceivedRequest(String method, List<Map<String, Object>> events) {
            this.method = method;
            this.events = events;
        }
    }

    private HttpServer startEventsEndpoint(int status, Integer retryAfter, String responseBody) throws IOException {
        receivedRequests = new ArrayList<>();
        List<ReceivedRequest> requests = receivedRequests;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
            String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);
            List<Map<String, Object>> events;
            try {
                events = GSON.fromJson(bodyStr, EVENTS_LIST_TYPE);
            } catch (Exception e) {
                events = List.of();
            }
            requests.add(new ReceivedRequest(exchange.getRequestMethod(), events == null ? List.of() : events));
            if (retryAfter != null) {
                exchange.getResponseHeaders().add("Retry-After", String.valueOf(retryAfter));
            }
            byte[] responseBytes = responseBody == null ? new byte[0] : responseBody.getBytes(StandardCharsets.UTF_8);
            if (responseBody != null) {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
            }
            exchange.sendResponseHeaders(status, responseBytes.length == 0 ? -1 : responseBytes.length);
            if (responseBytes.length > 0) {
                exchange.getResponseBody().write(responseBytes);
            }
            exchange.close();
        });
        server.start();
        return server;
    }

    @Given("a local events endpoint that responds with status {int}")
    public void a_local_events_endpoint(int status) throws IOException {
        eventsServer = startEventsEndpoint(status, null, null);
        eventsUrl = "http://127.0.0.1:" + eventsServer.getAddress().getPort();
    }

    @Given("a local events endpoint that responds with status {int} and Retry-After {int}")
    public void a_local_events_endpoint_retry_after(int status, int retryAfter) throws IOException {
        eventsServer = startEventsEndpoint(status, retryAfter, null);
        eventsUrl = "http://127.0.0.1:" + eventsServer.getAddress().getPort();
    }

    @Given("a local events endpoint that responds with status {int} and config body")
    public void a_local_events_endpoint_with_config_body(int status, String docString) throws IOException {
        eventsServer = startEventsEndpoint(status, null, docString);
        eventsUrl = "http://127.0.0.1:" + eventsServer.getAddress().getPort();
    }

    @Given("a local features endpoint with config header")
    public void a_local_features_endpoint_with_config_header(String docString) throws IOException {
        String headerValue = docString.replace("\n", " ");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("ETag", "\"features-etag\"");
            exchange.getResponseHeaders().add("X-Featureflow-Sdk-Config", headerValue);
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        featuresServer = server;
        featuresUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Given("a local features endpoint whose features change on every request")
    public void a_local_features_endpoint_that_changes() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger version = new AtomicInteger(0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            int v = version.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("ETag", "\"features-v" + v + "\"");
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        featuresServer = server;
        featuresUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    // Port 9 (discard) never answers, so nothing is ever actually sent.
    @Given("an events client")
    public void an_events_client() {
        FeatureflowConfig config = FeatureflowConfig.builder().withFeatureEventUri("http://127.0.0.1:9").build();
        RestClient restClient = new RestClientImpl("test-api-key", config);
        eventsClient = new EventsClientImpl(config, restClient);
    }

    @Given("an events client with a summary capacity of {int}")
    public void an_events_client_with_capacity(int capacity) {
        an_events_client();
        eventsClient.setSummaryCapacity(capacity);
    }

    @Given("an events client pointed at the local endpoint")
    public void an_events_client_pointed_at_local() {
        FeatureflowConfig config = FeatureflowConfig.builder().withFeatureEventUri(eventsUrl).build();
        RestClient restClient = new RestClientImpl("test-api-key", config);
        eventsClient = new EventsClientImpl(config, restClient);
    }

    @Given("a disabled events client")
    public void a_disabled_events_client() {
        FeatureflowConfig config = FeatureflowConfig.builder().withFeatureEventUri("http://127.0.0.1:9").build();
        RestClient restClient = new RestClientImpl("test-api-key", config);
        eventsClient = new EventsClientImpl(config, restClient, true);
    }

    @When("{int} evaluate events are queued")
    public void n_evaluate_events_are_queued(int count) {
        for (int i = 0; i < count; i++) {
            eventsClient.evaluateEvent("feature-" + i, "on", new FeatureflowUser("user-" + i), false);
        }
    }

    @When("{int} evaluate events are queued for feature {string} variant {string} user {string}")
    public void n_evaluate_events_for_feature(int count, String featureKey, String variant, String userId) {
        for (int i = 0; i < count; i++) {
            eventsClient.evaluateEvent(featureKey, variant, new FeatureflowUser(userId), false);
        }
    }

    @When("{int} evaluate events are queued for tracked feature {string} variant {string} user {string}")
    public void n_evaluate_events_for_tracked_feature(int count, String featureKey, String variant, String userId) {
        for (int i = 0; i < count; i++) {
            eventsClient.evaluateEvent(featureKey, variant, new FeatureflowUser(userId), true);
        }
    }

    @When("the event queue is flushed")
    public void the_event_queue_is_flushed() {
        eventsClient.sendQueue();
    }

    private EventsClientImpl.SummarySnapshot findSummary(String featureKey, String variant) {
        for (EventsClientImpl.SummarySnapshot s : eventsClient.getSummarySnapshot()) {
            if (s.featureKey.equals(featureKey) && s.evaluatedVariant.equals(variant)) {
                return s;
            }
        }
        return null;
    }

    @Then("the pending summary should contain {int} entries")
    public void pending_summary_should_contain(int count) {
        eventually(() -> assertEquals(count, eventsClient.getPendingSummaryCount()));
    }

    @Then("the pending summary for feature {string} variant {string} should have {int} impressions")
    public void pending_summary_impressions(String featureKey, String variant, int impressions) {
        eventually(() -> {
            EventsClientImpl.SummarySnapshot entry = findSummary(featureKey, variant);
            assertNotNull(entry, "summary entry for " + featureKey + "/" + variant);
            assertEquals(impressions, entry.impressions);
        });
    }

    @Then("the pending summary entry for feature {string} variant {string} should include user {string}")
    public void pending_summary_includes_user(String featureKey, String variant, String userId) {
        eventually(() -> {
            EventsClientImpl.SummarySnapshot entry = findSummary(featureKey, variant);
            assertNotNull(entry, "summary entry for " + featureKey + "/" + variant);
            assertTrue(entry.userIds.contains(userId));
        });
    }

    @Then("the pending summary entry for feature {string} variant {string} should include no users")
    public void pending_summary_includes_no_users(String featureKey, String variant) {
        EventsClientImpl.SummarySnapshot entry = findSummary(featureKey, variant);
        assertNotNull(entry, "summary entry for " + featureKey + "/" + variant);
        assertEquals(0, entry.userIds.size());
    }

    @Then("the pending summary entry for feature {string} variant {string} should include only user {string}")
    public void pending_summary_includes_only_user(String featureKey, String variant, String userId) {
        EventsClientImpl.SummarySnapshot entry = findSummary(featureKey, variant);
        assertNotNull(entry, "summary entry for " + featureKey + "/" + variant);
        assertEquals(List.of(userId), entry.userIds);
    }

    @Then("the recorded evaluate event for {string} should have trackEvents {word}")
    public void recorded_evaluate_event_track_events(String featureKey, String expected) {
        Boolean actual = eventsClient.getLastTrackEvents(featureKey);
        assertNotNull(actual, "no evaluate event recorded for " + featureKey);
        assertEquals(Boolean.parseBoolean(expected), actual);
    }

    @Then("the events client should become disabled")
    public void events_client_should_become_disabled() {
        eventually(() -> assertTrue(eventsClient.isDisabled()));
    }

    @Then("queueing another evaluate event should leave the summary empty")
    public void queueing_another_event_leaves_summary_empty() {
        eventsClient.evaluateEvent("another-feature", "on", new FeatureflowUser("user-x"), false);
        assertEquals(0, eventsClient.getPendingSummaryCount());
    }

    @Then("the events client should be backing off")
    public void events_client_should_be_backing_off() {
        eventually(() -> assertTrue(eventsClient.isBackingOff()));
    }

    @Then("the local endpoint should have received {int} request")
    public void local_endpoint_received_n_requests(int count) throws InterruptedException {
        // Give a would-be second request time to arrive before asserting it did not.
        Thread.sleep(150);
        assertEquals(count, receivedRequests.size());
    }

    @Then("the local endpoint should have received a batch of {int} events")
    public void local_endpoint_received_batch(int count) {
        eventually(() -> {
            assertEquals(1, receivedRequests.size());
            assertEquals(count, receivedRequests.get(0).events.size());
        });
    }

    @Then("the batch should total {int} impressions for feature {string} variant {string}")
    public void batch_total_impressions(int impressions, String featureKey, String variant) {
        int total = 0;
        for (Map<String, Object> e : receivedRequests.get(0).events) {
            if (featureKey.equals(e.get("featureKey")) && variant.equals(e.get("evaluatedVariant"))) {
                total += ((Number) e.get("impressions")).intValue();
            }
        }
        assertEquals(impressions, total);
    }

    @Then("the batch should include users {string} and {string}")
    public void batch_should_include_users(String userA, String userB) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> e : receivedRequests.get(0).events) {
            Object user = e.get("user");
            if (user instanceof Map) {
                Object id = ((Map<?, ?>) user).get("id");
                if (id != null) {
                    ids.add(String.valueOf(id));
                }
            }
        }
        assertTrue(ids.contains(userA));
        assertTrue(ids.contains(userB));
        assertEquals(new HashSet<>(ids).size(), ids.size());
    }

    @Then("the batch events should not include an expectedVariant")
    public void batch_events_should_not_include_expected_variant() {
        for (Map<String, Object> e : receivedRequests.get(0).events) {
            assertFalse(e.containsKey("expectedVariant"));
        }
    }

    @Given("a Featureflow client with the stored features")
    public void a_featureflow_client_with_stored_features(DataTable table) {
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withFeatureEventUri("http://127.0.0.1:9")
                .withPollingInterval(0)
                .withWaitForStartup(0)
                .build();
        // A short (<=10 char) apiKey stops the polling client fetching, and interval 0
        // disables polling and lazy refresh, so the client runs entirely offline — the
        // stored features are injected directly into its cache below.
        featureflowClient = FeatureflowClient.builder("test-key").withConfig(config).build();
        eventsClient = (EventsClientImpl) TestAccessor.getEventHandler(featureflowClient);

        Map<String, FeatureControl> controls = new HashMap<>();
        for (Map<String, String> row : table.asMaps()) {
            FeatureControl control = new FeatureControl();
            control.key = row.get("key");
            control.enabled = "true".equals(row.get("enabled"));
            control.offVariantKey = row.get("offVariantKey");
            control.trackEvents = "true".equals(row.get("trackEvents"));
            Rule rule = new Rule();
            rule.setAudience(null);
            rule.setVariantSplits(List.of(new VariantSplit(row.get("defaultVariant"), 100L)));
            control.rules = new ArrayList<>(List.of(rule));
            controls.put(control.key, control);
        }
        TestAccessor.setFeatureControls(featureflowClient, controls);
    }

    @When("evaluateAll is called for user {string}")
    public void evaluate_all_is_called_for_user(String userId) {
        evaluatedFeatures = featureflowClient.evaluateAll(new FeatureflowUser(userId));
    }

    @When("evaluate {string} is called for user {string} and isOn is checked")
    public void evaluate_is_called_and_ison_checked(String key, String userId) {
        featureflowClient.evaluate(key, userId).isOn();
    }

    @Then("the evaluated features should be")
    public void the_evaluated_features_should_be(DataTable table) {
        Map<String, String> expected = new HashMap<>();
        for (Map<String, String> row : table.asMaps()) {
            expected.put(row.get("key"), row.get("variant"));
        }
        assertEquals(expected, evaluatedFeatures);
    }

    @Then("no evaluate events should have been recorded")
    public void no_evaluate_events_should_have_been_recorded() {
        assertEquals(0, eventsClient.getPendingSummaryCount());
    }

    @Then("{int} evaluate event should have been recorded")
    public void n_evaluate_events_should_have_been_recorded(int count) {
        assertEquals(count, eventsClient.getPendingSummaryCount());
    }

    // --- goals.feature ---

    @When("goal {string} is tracked for user {string}")
    public void goal_is_tracked_for_user(String goalKey, String userId) {
        eventsClient.trackEvent(goalKey, new FeatureflowUser(userId), null);
    }

    @When("goal {string} is tracked for user {string} with value {float}")
    public void goal_is_tracked_with_value(String goalKey, String userId, float value) {
        eventsClient.trackEvent(goalKey, new FeatureflowUser(userId), value);
    }

    @When("goal {string} is tracked for user {string} with details")
    @SuppressWarnings("unchecked")
    public void goal_is_tracked_with_details(String goalKey, String userId, String docString) {
        Map<String, Object> details = GSON.fromJson(docString, Map.class);
        eventsClient.trackEvent(goalKey, new FeatureflowUser(userId), details);
    }

    private Event findGoal(String goalKey) {
        for (Event e : eventsClient.getPendingGoals()) {
            if (goalKey.equals(e.getGoalKey())) {
                return e;
            }
        }
        return null;
    }

    @Then("the pending goals should contain {int} events")
    public void pending_goals_should_contain(int count) {
        assertEquals(count, eventsClient.getPendingGoalCount());
    }

    @Then("the pending goal {string} should have user {string}")
    public void pending_goal_should_have_user(String goalKey, String userId) {
        Event goal = findGoal(goalKey);
        assertNotNull(goal, "pending goal " + goalKey);
        assertEquals(Event.GOAL_EVENT, goal.getType());
        assertEquals(userId, goal.getUser().getId());
        assertNotNull(goal.getTimestamp());
    }

    @Then("the pending goal {string} should have value {float}")
    public void pending_goal_should_have_value(String goalKey, float value) {
        Event goal = findGoal(goalKey);
        assertNotNull(goal, "pending goal " + goalKey);
        assertEquals((double) value, goal.getValue(), 0.0001);
    }

    @Then("the pending goal {string} should have data")
    @SuppressWarnings("unchecked")
    public void pending_goal_should_have_data(String goalKey, String docString) {
        Event goal = findGoal(goalKey);
        assertNotNull(goal, "pending goal " + goalKey);
        Map<String, Object> expected = GSON.fromJson(docString, Map.class);
        assertEquals(expected, goal.getData());
    }

    @Then("the batch should include a goal event {string} with type {string} and no featureKey")
    public void batch_should_include_goal_event(String goalKey, String type) {
        Map<String, Object> row = null;
        for (Map<String, Object> e : receivedRequests.get(0).events) {
            if (goalKey.equals(e.get("goalKey"))) {
                row = e;
                break;
            }
        }
        assertNotNull(row, "batch row for goal " + goalKey);
        assertEquals(type, row.get("type"));
        assertFalse(row.containsKey("featureKey"));
    }

    // --- sdk_config.feature ---

    @When("the server config is applied")
    @SuppressWarnings("unchecked")
    public void the_server_config_is_applied(String docString) {
        Map<String, Object> config = GSON.fromJson(docString, Map.class);
        eventsClient.applyServerConfig(config);
    }

    @Then("the events client send interval should be {int} seconds")
    public void events_client_send_interval_should_be(int seconds) {
        eventually(() -> assertEquals(seconds, eventsClient.getSendIntervalSeconds()));
    }

    @Then("the events client should become suspended")
    public void events_client_should_become_suspended() {
        eventually(() -> assertTrue(eventsClient.isSuspended()));
    }

    @Then("the events client should not be suspended")
    public void events_client_should_not_be_suspended() {
        assertFalse(eventsClient.isSuspended());
    }

    @Then("every pending entry should have {int} impression and user {string}")
    public void every_pending_entry_should_have(int impressions, String userId) {
        List<EventsClientImpl.SummarySnapshot> snapshot = eventsClient.getSummarySnapshot();
        assertTrue(snapshot.size() > 0);
        for (EventsClientImpl.SummarySnapshot s : snapshot) {
            assertEquals(impressions, s.impressions);
            assertEquals(List.of(userId), s.userIds);
        }
    }

    @Given("a Featureflow client pointed at the local features endpoint")
    public void a_featureflow_client_pointed_at_local_features_endpoint() {
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withPollingUri(featuresUrl)
                .withFeatureEventUri("http://127.0.0.1:9")
                .withRegisterFeatureUri("http://127.0.0.1:9")
                .withPollingInterval(0)
                .withWaitForStartup(0)
                .build();
        featureflowClient = FeatureflowClient.builder("test-api-key-12345").withConfig(config).build();
        eventsClient = (EventsClientImpl) TestAccessor.getEventHandler(featureflowClient);
    }

    @Given("a polling Featureflow client pointed at the local features endpoint")
    public void a_polling_featureflow_client_pointed_at_local_features_endpoint() {
        updatedEventCount = 0;
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withPollingUri(featuresUrl)
                .withPollingInterval(20)
                .build();
        pollingClient = new FeatureflowPollingClient("test-api-key-12345", config, new SimpleMemoryFeatureCache(),
                new HashMap<>(), null, () -> updatedEventCount++);
    }

    @When("the features are refreshed")
    public void the_features_are_refreshed() {
        pollingClient.refresh();
    }

    @Then("the polling interval should become {int} seconds")
    public void the_polling_interval_should_become(int seconds) {
        eventually(() -> assertEquals(seconds * 1000, pollingClient.getPollingIntervalMillis()));
    }

    @Then("the polling interval should remain {int}")
    public void the_polling_interval_should_remain(int value) {
        assertEquals(value, TestAccessor.getPollingClient(featureflowClient).getPollingIntervalMillis());
    }

    @Then("the client should have emitted {int} updated event")
    public void the_client_should_have_emitted_updated_event(int count) {
        eventually(() -> assertEquals(count, updatedEventCount));
    }
}
