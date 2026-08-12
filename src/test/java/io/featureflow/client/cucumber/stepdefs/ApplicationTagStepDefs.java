package io.featureflow.client.cucumber.stepdefs;

import com.sun.net.httpserver.HttpServer;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.core.FeatureflowPollingClient;
import io.featureflow.client.core.RestClientImpl;
import io.featureflow.client.core.SimpleMemoryFeatureCache;
import io.featureflow.client.model.Feature;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Step definitions for application_tag.feature: the X-Featureflow-Application header on
 * features and register requests, mirroring featureflow-node-sdk's step definitions. The
 * tag flows config → sanitisation ({@link FeatureflowConfig.Builder#withApplication}) →
 * transport header, so these steps drive the real polling/rest clients against a local
 * capturing endpoint rather than asserting on config state.
 */
public class ApplicationTagStepDefs {

    private static final class CapturedRequest {
        final String path;
        final String applicationHeader;

        CapturedRequest(String path, String applicationHeader) {
            this.path = path;
            this.applicationHeader = applicationHeader;
        }
    }

    private HttpServer server;
    private String serverUrl;
    private final List<CapturedRequest> captured = new ArrayList<>();
    private FeatureflowPollingClient pollingClient;

    @After
    public void tearDown() throws IOException {
        if (pollingClient != null) {
            pollingClient.close();
            pollingClient = null;
        }
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void startCapturingServer() throws IOException {
        captured.clear();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            captured.add(new CapturedRequest(
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("X-Featureflow-Application")));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        serverUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

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

    @Given("a features endpoint capturing request headers")
    public void a_features_endpoint_capturing_request_headers() throws IOException {
        startCapturingServer();
    }

    @Given("an events endpoint capturing request headers")
    public void an_events_endpoint_capturing_request_headers() throws IOException {
        startCapturingServer();
    }

    private void startPollingClient(String application) {
        FeatureflowConfig.Builder builder = FeatureflowConfig.builder()
                .withPollingUri(serverUrl + "/api/sdk/v1/features")
                .withPollingInterval(3600);
        if (application != null) {
            builder.withApplication(application);
        }
        // A valid-length key triggers the initial fetch on construction.
        pollingClient = new FeatureflowPollingClient(
                "test-api-key-12345", builder.build(), new SimpleMemoryFeatureCache(), new HashMap<>());
    }

    @When("a client is initialised against it with application {string}")
    public void a_client_with_application(String application) {
        startPollingClient(application);
    }

    @When("a client is initialised against it with no application")
    public void a_client_with_no_application() {
        startPollingClient(null);
    }

    @When("a client with a registered feature is initialised against it with application {string}")
    public void a_client_with_registered_feature(String application) throws IOException {
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withRegisterFeatureUri(serverUrl + "/api/sdk/v1/register")
                .withApplication(application)
                .build();
        new RestClientImpl("test-api-key-12345", config)
                .registerFeatureControls(List.of(new Feature("my-flag")));
    }

    @Then("the captured features request has X-Featureflow-Application {string}")
    public void captured_features_request_has_application(String expected) {
        eventually(() -> {
            assertFalse(captured.isEmpty(), "no features request captured");
            assertEquals(expected, captured.get(0).applicationHeader);
        });
    }

    @Then("the captured features request has no X-Featureflow-Application header")
    public void captured_features_request_has_no_application() {
        eventually(() -> {
            assertFalse(captured.isEmpty(), "no features request captured");
            assertNull(captured.get(0).applicationHeader);
        });
    }

    @Then("the captured register request has X-Featureflow-Application {string}")
    public void captured_register_request_has_application(String expected) {
        eventually(() -> {
            CapturedRequest register = captured.stream()
                    .filter(request -> request.path.contains("register"))
                    .findFirst().orElse(null);
            assertEquals(expected, register == null ? null : register.applicationHeader);
        });
    }
}
