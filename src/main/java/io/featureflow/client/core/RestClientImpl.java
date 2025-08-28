package io.featureflow.client.core;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.model.Event;
import io.featureflow.client.model.Feature;
import org.apache.hc.client5.http.cache.HttpCacheContext;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.cache.CacheConfig;
import org.apache.hc.client5.http.impl.cache.CachingHttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Updated to use HttpClient 5.x
 */
public class RestClientImpl implements RestClient {

    private static final String APPLICATION_JSON = "application/json";
    private static final String UTF_8 = "UTF-8";

    private final String apiKey;
    private final FeatureflowConfig config;
    private CloseableHttpClient client = null;

    private Gson gson = new GsonBuilder()
        .registerTypeAdapter(DateTime.class, new JsonSerializer<DateTime>(){
            @Override
            public JsonElement serialize(DateTime json, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(ISODateTimeFormat.dateTime().print(json));
            }
        }).create();

    private static final Logger logger = LoggerFactory.getLogger(RestClientImpl.class);

    public RestClientImpl(String apiKey, FeatureflowConfig config) {
        this.apiKey = apiKey;
        this.config = config;
        client = createHttpClient();
    }

    /**
     * Register any code defined feature controls as available and retrieve a list of features form the server
     * @param featureRegistrations list of features to register
     * @throws IOException cannot connect to register
     */
    @Override
    public void registerFeatureControls(List<Feature> featureRegistrations) throws IOException{
        logger.info("Registering features with featureflow");
        URI uri = URI.create(config.getRegisterFeatureUri());
        HttpCacheContext context = HttpCacheContext.create();
        HttpPut request = createPutRequest(uri, gson.toJson(featureRegistrations));
        CloseableHttpResponse response = null;
        try {
            logger.debug("Putting: " + request);
            response = client.execute(request, context);
            if(response.getCode() != HttpStatus.SC_OK){
                logger.error("Problem registering controls: " + response.getReasonPhrase());
                throw new IOException("Problem registering controls " + response.getReasonPhrase());
            }
        }
        finally {
            try {
                if (response != null) response.close();
            } catch (IOException e) {}
        }
    }

    @Override
    public void postEvents(List<? extends Event> events) {
        URI uri = URI.create(config.getFeatureEventUri());
        CloseableHttpResponse response = null;
        Type type = new TypeToken<List<Event>>() {}.getType();
        String json = gson.toJson(events, type);
        HttpPost request = createPostRequest(uri, json);
        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        try {
            client = createHttpClient();
            response = client.execute(request);
        } catch (IOException e) {
            logger.error("Network exception posting events", e);
        } finally {
            try {
                if (response != null) response.close();
            } catch (IOException e) {
                logger.error("Cannot close stream", e);
            }
        }
    }

    private HttpPut createPutRequest(URI uri, String data) {
        try {
            HttpPut request = new HttpPut(uri);
            setRequestVals(request, data);
            return request;
        } catch (Exception e) {
            logger.error("Problem in PUT request ", e);
            return null;
        }
    }

    private HttpPost createPostRequest(URI uri, String data) {
        try {
            HttpPost request = new HttpPost(uri);
            setRequestVals(request, data);
            return request;
        } catch (Exception e) {
            logger.error("Problem in POST request ", e);
            return null;
        }
    }

    private void setRequestVals(HttpMessage request, String data) {
        if (request instanceof HttpMessage) {
            StringEntity params = new StringEntity(data, ContentType.APPLICATION_JSON);

            request.addHeader("content-type", "application/json");
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
            request.addHeader("Accept-Language", "en-US,en;q=0.8");

            if (request instanceof HttpPut httpPut) {
                httpPut.setEntity(params);
            } else if (request instanceof HttpPost httpPost) {
                httpPost.setEntity(params);
            }

            request.addHeader("Authorization", "Bearer " + apiKey);
            request.addHeader("X-Featureflow-Client", "JavaClient/" + FeatureflowConfig.VERSION);
        }
    }

    private CloseableHttpClient createHttpClient() {
        CloseableHttpClient client;
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
                    // Do not retry if over max retry count
                    return false;
                }
                if (exception instanceof org.apache.hc.client5.http.ConnectTimeoutException) {
                    // Connection refused
                    return false;
                }
                if (exception instanceof InterruptedIOException) {
                    // Timeout
                    return false;
                }
                if (exception instanceof UnknownHostException) {
                    // Unknown host
                    return false;
                }
                if (exception instanceof SSLException) {
                    // SSL handshake exception
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
                .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(config.getSocketTimeout()))
                .setProxy(config.getHttpProxyHost())
                .build();

        client = CachingHttpClients.custom()
                .setCacheConfig(cacheConfig)
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryStrategy(myRetryHandler) // Using standard retry strategy
                .setRetryStrategy(serviceUnavailableRetryStrategy) // This will override the previous one
                .build();

        return client;
    }
}
