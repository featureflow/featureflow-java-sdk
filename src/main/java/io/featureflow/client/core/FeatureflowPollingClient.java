package io.featureflow.client.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.cache.CacheConfig;
import org.apache.hc.client5.http.impl.cache.CachingHttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.featureflow.client.FeatureControlCallbackHandler;
import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.model.FeatureControl;

/**
 * Polling-based client for fetching feature controls from Featureflow.
 * This client polls the Featureflow API at regular intervals to get feature updates.
 * It uses ETags to optimize requests and minimize data transfer.
 */
public class FeatureflowPollingClient implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(FeatureflowPollingClient.class);
    
    // Default configuration values
    private static final int DEFAULT_INTERVAL = 60000; // 60 seconds
    private static final int MIN_SERVER_POLL_INTERVAL_SECONDS = 5;
    private static final int MAX_SERVER_POLL_INTERVAL_SECONDS = 3600;
    private static final String CLIENT_VERSION = "JavaClient/" + FeatureflowConfig.VERSION;
    private static final String SDK_CONFIG_HEADER = "X-Featureflow-Sdk-Config";

    private final String apiKey;
    private final FeatureflowConfig config;
    private final FeatureControlCache repository;
    private final Map<CallbackEvent, List<FeatureControlCallbackHandler>> callbacks;
    private final CloseableHttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final Gson gson;
    // Called with the parsed X-Featureflow-Sdk-Config header (server-driven SDK config)
    // whenever a features response carries one — on 304s too, which is what a polling
    // client mostly sees.
    private final Consumer<Map<String, Object>> onSdkConfig;
    // Called when a poll observes changed features (a 200 with a new ETag after the
    // initial load).
    private final Runnable onUpdate;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private String etag = "";
    private volatile int pollingInterval;
    private ScheduledFuture<?> pollingTask;
    
    // Type tokens for JSON deserialization
    private final Type mapOfFeatureControlsType = new TypeToken<Map<String, FeatureControl>>() {}.getType();
    
    /**
     * Creates a new FeatureflowPollingClient.
     *
     * @param apiKey The Featureflow API key
     * @param config The Featureflow configuration
     * @param repository The feature control repository
     * @param callbacks Map of event callbacks
     */
    public FeatureflowPollingClient(String apiKey,
                                   FeatureflowConfig config,
                                   FeatureControlCache repository,
                                   Map<CallbackEvent, List<FeatureControlCallbackHandler>> callbacks) {
        this(apiKey, config, repository, callbacks, null, null);
    }

    public FeatureflowPollingClient(String apiKey,
                                   FeatureflowConfig config,
                                   FeatureControlCache repository,
                                   Map<CallbackEvent, List<FeatureControlCallbackHandler>> callbacks,
                                   Consumer<Map<String, Object>> onSdkConfig,
                                   Runnable onUpdate) {
        this.apiKey = apiKey;
        this.config = config;
        this.repository = repository;
        this.callbacks = callbacks;
        this.onSdkConfig = onSdkConfig;
        this.onUpdate = onUpdate;
        this.httpClient = createHttpClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "featureflow-polling-client");
            t.setDaemon(true);
            return t;
        });
        this.gson = new Gson();
        
        // Set polling interval from config, with validation
        this.pollingInterval = getPollingInterval(config);
        
        // Start polling if API key is valid
        if (isValidApiKey(apiKey)) {
            startPolling();
        } else {
            logger.warn("API key is missing or too short. Features will not be fetched.");
        }
    }
    
    /**
     * Starts the polling process.
     */
    private void startPolling() {
        // Initial fetch
        getFeatures();
        
        // Schedule periodic polling if interval > 0
        if (pollingInterval > 0) {
            pollingTask = scheduler.scheduleWithFixedDelay(
                this::getFeatures,
                pollingInterval,
                pollingInterval,
                TimeUnit.MILLISECONDS
            );
            logger.info("Started polling for feature updates every {} ms", pollingInterval);
        } else {
            logger.info("Polling interval set to 0. Featureflow will NOT poll for feature changes.");
        }
    }
    
    /**
     * Fetches features from the Featureflow API.
     */
    private void getFeatures() {
        if (closed.get()) {
            return;
        }
        
        try {
            URI uri = URI.create(getPollingUri());
            HttpGet request = createGetRequest(uri);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();

                // The config header is delivered on both 200 and 304 responses — a
                // polling client mostly sees 304s, so it must be read here regardless
                // of status.
                handleSdkConfigHeader(response.getFirstHeader(SDK_CONFIG_HEADER));

                if (statusCode == HttpStatus.SC_OK) {
                    String previousEtag = this.etag;

                    // Update ETag for future requests
                    Header etagHeader = response.getFirstHeader(HttpHeaders.ETAG);
                    if (etagHeader != null) {
                        this.etag = etagHeader.getValue();
                    }

                    // The underlying CachingHttpClients transport can transparently
                    // reconstruct a 200 (with the cached body) from a successfully
                    // revalidated 304 — observed in practice once the Vary-derived
                    // cache variant lookup mismatches (e.g. an Accept-Encoding header
                    // whose element order differs from what was cached), at which point
                    // the caching layer stops passing the raw 304 through to the
                    // caller. So the HTTP status alone cannot be trusted to mean "the
                    // features changed" — the ETag comparison is the reliable signal.
                    boolean firstFetch = previousEtag.isEmpty();
                    boolean changed = firstFetch || !previousEtag.equals(this.etag);

                    if (changed) {
                        String responseBody = new String(EntityUtils.toByteArray(response.getEntity()));
                        Map<String, FeatureControl> controls = gson.fromJson(responseBody, mapOfFeatureControlsType);

                        if (controls != null) {
                            logger.debug("Updating features from polling response");
                            updateFeatures(controls);
                        }
                    } else {
                        logger.debug("No feature changes detected (ETag unchanged: {})", this.etag);
                    }

                    // Mark as initialized on first successful fetch
                    if (!initialized.getAndSet(true)) {
                        logger.info("Featureflow polling client initialized.");
                    }

                    // Notify only on an actual change after the initial load — not on
                    // the first fetch and not when the ETag is unchanged.
                    if (!firstFetch && changed && onUpdate != null) {
                        onUpdate.run();
                    }

                } else if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
                    // No changes since last request (ETag optimization)
                    logger.debug("No feature changes detected (304 Not Modified)");

                } else if (statusCode >= 400) {
                    logger.warn("Request for features failed with response status {}", statusCode);
                }

            } catch (Exception e) {
                logger.error("Error fetching features", e);
            }
            
        } catch (Exception e) {
            logger.error("Failed to fetch features", e);
        }
    }
    
    /**
     * Updates features in the repository and triggers callbacks.
     */
    private void updateFeatures(Map<String, FeatureControl> controls) {
        for (Map.Entry<String, FeatureControl> entry : controls.entrySet()) {
            String featureKey = entry.getKey();
            FeatureControl featureControl = entry.getValue();
            
            if (logger.isDebugEnabled()) {
                logger.debug("Updating feature {} enabled: {}", featureKey, featureControl.enabled);
            }
            
            repository.update(featureKey, featureControl);
            
            // Trigger update callbacks
            if (callbacks != null && callbacks.get(CallbackEvent.UPDATED_FEATURE) != null) {
                for (FeatureControlCallbackHandler callback : callbacks.get(CallbackEvent.UPDATED_FEATURE)) {
                    try {
                        callback.onUpdate(featureControl);
                    } catch (Exception e) {
                        logger.error("Error in feature update callback", e);
                    }
                }
            }
        }
    }
    
    /**
     * Creates an HTTP GET request with proper headers.
     */
    private HttpGet createGetRequest(URI uri) {
        HttpGet request = new HttpGet(uri);
        
        // Set headers
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        request.setHeader("X-Featureflow-Client", CLIENT_VERSION);
        if (config.getApplication() != null) {
            // Write-only telemetry: never affects the response, so it cannot fragment the CDN cache.
            request.setHeader("X-Featureflow-Application", config.getApplication());
        }
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        
        // Add ETag for conditional requests
        if (!etag.isEmpty()) {
            request.setHeader(HttpHeaders.IF_NONE_MATCH, etag);
        }
        
        return request;
    }
    
    /**
     * Creates an HTTP client with appropriate configuration.
     */
    private CloseableHttpClient createHttpClient() {
        // Create HTTP client with same configuration as RestClientImpl
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(100);
        manager.setDefaultMaxPerRoute(20);

        CacheConfig cacheConfig = CacheConfig.custom()
                .setMaxCacheEntries(1000)
                .setMaxObjectSize(131072)
                .setSharedCache(false)
                .build();

        // Custom retry strategy for service unavailable errors
        org.apache.hc.client5.http.HttpRequestRetryStrategy serviceUnavailableRetryStrategy = new org.apache.hc.client5.http.HttpRequestRetryStrategy() {
            @Override
            public boolean retryRequest(HttpRequest request, IOException exception, int executionCount, HttpContext context) {
                return false; // Only retry on 502 response, handled in other method
            }

            @Override
            public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
                int statusCode = response.getCode();
                return statusCode == 502 && executionCount < 1000;
            }

            @Override
            public TimeValue getRetryInterval(HttpRequest request, IOException exception, int executionCount, HttpContext context) {
                return TimeValue.of(5, TimeUnit.SECONDS);
            }

            @Override
            public TimeValue getRetryInterval(HttpResponse response, int executionCount, HttpContext context) {
                return TimeValue.of(5, TimeUnit.SECONDS);
            }
        };

        // Custom retry strategy for standard request retries
        org.apache.hc.client5.http.HttpRequestRetryStrategy myRetryHandler = new org.apache.hc.client5.http.HttpRequestRetryStrategy() {
            @Override
            public boolean retryRequest(HttpRequest request, IOException exception, int executionCount, HttpContext context) {
                if (executionCount >= 5) {
                    return false;
                }
                if (exception instanceof org.apache.hc.client5.http.ConnectTimeoutException) {
                    return false;
                }
                if (exception instanceof InterruptedIOException) {
                    return false;
                }
                if (exception instanceof UnknownHostException) {
                    return false;
                }
                if (exception instanceof SSLException) {
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest originalRequest = clientContext.getRequest();
                boolean idempotent = !(originalRequest instanceof BasicClassicHttpRequest);
                return idempotent;
            }

            @Override
            public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
                return false; // Only retry on exceptions
            }

            @Override
            public TimeValue getRetryInterval(HttpRequest request, IOException exception, int executionCount, HttpContext context) {
                return TimeValue.of(1, TimeUnit.SECONDS);
            }

            @Override
            public TimeValue getRetryInterval(HttpResponse response, int executionCount, HttpContext context) {
                return TimeValue.of(1, TimeUnit.SECONDS);
            }
        };

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getConnectTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(config.getSocketTimeout()))
                .setProxy(config.getHttpProxyHost())
                .build();

        return CachingHttpClients.custom()
                .setCacheConfig(cacheConfig)
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryStrategy(myRetryHandler)
                .setRetryStrategy(serviceUnavailableRetryStrategy)
                .build();
    }
    
    /**
     * Gets the polling interval from config, with validation. An explicit 0 disables
     * polling entirely and is never overridden (including by server config, below) —
     * distinct from "not configured", which falls back to the default.
     */
    private int getPollingInterval(FeatureflowConfig config) {
        int interval = config.getPollingInterval();
        if (interval == 0) {
            return 0;
        }
        // Validate interval - must be at least 20 seconds
        if (interval > 0 && interval < 20000) {
            logger.warn("Polling interval {}ms is too short, using minimum of 20 seconds", interval);
            return 20000;
        }
        return interval > 0 ? interval : DEFAULT_INTERVAL;
    }

    /**
     * Applies server-driven config: {@code pollIntervalSeconds} retunes the features
     * polling period fleet-wide. Invalid values are ignored, and a locally disabled
     * poller (interval 0) is never re-enabled.
     */
    private void handleSdkConfigHeader(Header header) {
        if (header == null) {
            return;
        }
        Map<String, Object> parsed;
        try {
            parsed = gson.fromJson(header.getValue(), Map.class);
        } catch (Exception e) {
            logger.debug("ignoring malformed {} header: {}", SDK_CONFIG_HEADER, header.getValue());
            return;
        }
        if (parsed == null) {
            return;
        }
        applyServerConfig(parsed);
        if (onSdkConfig != null) {
            onSdkConfig.accept(parsed);
        }
    }

    private void applyServerConfig(Map<String, Object> config) {
        Object seconds = config.get("pollIntervalSeconds");
        if (seconds instanceof Number) {
            double value = ((Number) seconds).doubleValue();
            if (value >= MIN_SERVER_POLL_INTERVAL_SECONDS && value <= MAX_SERVER_POLL_INTERVAL_SECONDS) {
                setPollInterval((int) value);
            }
        }
    }

    private synchronized void setPollInterval(int seconds) {
        int intervalMs = seconds * 1000;
        if (pollingInterval <= 0 || intervalMs == pollingInterval) {
            return;
        }
        logger.debug("Polling interval changed by server config: {}ms -> {}ms", pollingInterval, intervalMs);
        pollingInterval = intervalMs;
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = scheduler.scheduleWithFixedDelay(this::getFeatures, pollingInterval, pollingInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Validates the API key.
     */
    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && apiKey.length() > 10;
    }

    /**
     * Gets the polling URI from config.
     */
    private String getPollingUri() {
        return config.getPollingUri();
    }

    /**
     * Checks if the client is initialized.
     */
    public boolean initialized() {
        return initialized.get();
    }

    /** Triggers an immediate synchronous fetch, bypassing the scheduled timer. */
    public void refresh() {
        getFeatures();
    }

    /** The current effective polling interval, in milliseconds (0 means disabled). */
    public int getPollingIntervalMillis() {
        return pollingInterval;
    }
    
    @Override
    public void close() throws IOException {
        if (closed.getAndSet(true)) {
            return; // Already closed
        }
        
        logger.info("Closing Featureflow polling client");
        
        // Cancel polling task
        if (pollingTask != null) {
            pollingTask.cancel(true);
        }
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Close HTTP client
        if (httpClient != null) {
            httpClient.close();
        }
        
        // Close repository
        if (repository != null) {
            repository.close();
        }
    }
}
