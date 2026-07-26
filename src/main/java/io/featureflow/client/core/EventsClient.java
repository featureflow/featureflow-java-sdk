package io.featureflow.client.core;

import io.featureflow.client.FeatureflowUser;

import java.io.Closeable;
import java.util.Map;

public interface EventsClient extends Closeable {
    /**
     * Record an evaluation for summarisation. {@code trackEvents} is the evaluated
     * feature's per-flag exposure fidelity flag (dormant server-side).
     */
    void evaluateEvent(String featureKey, String evaluatedVariant, FeatureflowUser user, boolean trackEvents);

    /**
     * Record a goal (track) event, sent raw. {@code details} is a {@link Number} (the
     * metric value), a {@code Map} with an optional numeric {@code "value"} plus custom
     * fields, or {@code null} — the OpenFeature tracking API shape.
     */
    void trackEvent(String goalKey, FeatureflowUser user, Object details);

    /**
     * Apply server-driven config ({@code eventsEnabled}/{@code mode}/{@code flushIntervalSeconds}),
     * delivered via the X-Featureflow-Sdk-Config response header on /features or the
     * /events response body. See SDK-CONFIG.md for the full contract.
     */
    void applyServerConfig(Map<String, Object> config);

    /** Flush pending summaries/goals immediately, bypassing the scheduled timer. */
    void sendQueue();
}
