package io.featureflow.client.core;

import com.sun.net.httpserver.HttpServer;
import io.featureflow.client.FeatureflowConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Change-detection on the features poll must key off the ETag, not the HTTP status
 * code. In production, Apache HttpClient's CachingHttpClients transport (used by
 * {@link FeatureflowPollingClient}) can transparently reconstruct a full 200 response
 * (with the cached body) from a successfully revalidated 304 — observed once the
 * Vary-derived cache variant lookup mismatches (e.g. an Accept-Encoding header whose
 * element order differs from what was cached) — so a raw 304 is not guaranteed to
 * reach the caller even when nothing changed.
 */
public class FeatureflowPollingClientEtagTest {
    private HttpServer server;
    private FeatureflowPollingClient pollingClient;

    @AfterEach
    public void tearDown() throws IOException {
        if (pollingClient != null) {
            pollingClient.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void secondPollSendsIfNoneMatchAndHandlesAGenuine304() throws IOException {
        AtomicInteger requestCount = new AtomicInteger(0);
        AtomicReference<String> secondRequestIfNoneMatch = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            int n = requestCount.incrementAndGet();
            if (n == 1) {
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.getResponseHeaders().add("ETag", "\"v1\"");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                secondRequestIfNoneMatch.set(exchange.getRequestHeaders().getFirst("If-None-Match"));
                if ("\"v1\"".equals(secondRequestIfNoneMatch.get())) {
                    exchange.sendResponseHeaders(304, -1);
                } else {
                    byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.getResponseHeaders().add("ETag", "\"v1\"");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
            }
            exchange.close();
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();

        FeatureflowConfig config = FeatureflowConfig.builder()
                .withPollingUri(url)
                .withPollingInterval(0) // poll only via explicit refresh() below
                .build();
        pollingClient = new FeatureflowPollingClient("test-api-key-12345", config, new SimpleMemoryFeatureCache(), new HashMap<>());

        // Constructor already performed poll #1.
        assertEquals(1, requestCount.get());

        pollingClient.refresh(); // poll #2

        assertEquals(2, requestCount.get());
        assertEquals("\"v1\"", secondRequestIfNoneMatch.get(), "poll #2 should have sent If-None-Match with poll #1's ETag");
    }

    @Test
    public void unchangedEtagDoesNotReTriggerUpdateEvenOnA200() throws IOException {
        // Reproduces the production behaviour directly: the transport hands back a 200
        // with the same ETag/body as last time (what CachingHttpClients can do after a
        // successful revalidation) — the SDK must recognise "nothing changed" from the
        // ETag alone and must not fire the update callback again.
        AtomicInteger onUpdateCount = new AtomicInteger(0);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("ETag", "\"same-etag\"");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();

        FeatureflowConfig config = FeatureflowConfig.builder()
                .withPollingUri(url)
                .withPollingInterval(0)
                .build();
        pollingClient = new FeatureflowPollingClient("test-api-key-12345", config, new SimpleMemoryFeatureCache(),
                new HashMap<>(), null, onUpdateCount::incrementAndGet);

        pollingClient.refresh(); // poll #2: same ETag as poll #1 (from the constructor), status 200 both times

        assertEquals(0, onUpdateCount.get(), "onUpdate must not fire when the ETag is unchanged, regardless of HTTP status");
    }
}
