package io.featureflow.client.core;

/**
 * The outcome of a POST to the events endpoint, carrying just enough of the HTTP
 * response for {@link EventsClientImpl} to act on server-driven behaviour: permanent
 * disable on 401/403, backoff on 429 (via the Retry-After header), and server-driven
 * SDK config delivered as the response body on 200.
 */
public class EventsPostResult {
    public final int statusCode;
    public final String retryAfterHeader;
    public final String body;

    public EventsPostResult(int statusCode, String retryAfterHeader, String body) {
        this.statusCode = statusCode;
        this.retryAfterHeader = retryAfterHeader;
        this.body = body;
    }
}
