package io.featureflow.client.core;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.model.Event;
import io.featureflow.client.model.Feature;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
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

/**
 * Created by oliver on 26/05/2016.
 */
public class RestClient {

    public static final String VERSION = "0.0.10-SNAPSHOT";
    public static final String APPLICATION_JSON = "application/json";
    public static final String UTF_8 = "UTF-8";
    public static final String API_V1_EVENTS = "/api/v1/events";
    public static final String HTTPS = "https";
    public static final int PORT = 443;

    private final String apiKey;
    private final FeatureflowConfig config;
    private CloseableHttpClient client = null;
    Gson gson =  new GsonBuilder()
        .registerTypeAdapter(DateTime.class, new JsonSerializer<DateTime>(){
            @Override
            public JsonElement serialize(DateTime json, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(ISODateTimeFormat.dateTime().print(json));
            }
        }).create();

    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    public RestClient(String apiKey, FeatureflowConfig config) {
        this.apiKey = apiKey;
        this.config = config;
        client = createHttpClient();
    }

    /**
     * Register any code defined feature controls as available and retrieve a list of features form the server
     * @param featureRegistrations list of features to register
     * @throws IOException cannot connect to register
     */
    public void registerFeatureControls(List<Feature> featureRegistrations) throws IOException{
        logger.info("Registering features with featureflow");
        HttpCacheContext context = HttpCacheContext.create();
        String resource = FeatureflowConfig.REGISTER_REST_PATH;
        HttpPut request = putRequest(apiKey, resource, gson.toJson(featureRegistrations));
        CloseableHttpResponse response = null;
        try {
            logger.debug("Putting: " + request);
            response = client.execute(request, context);
            if(response.getStatusLine().getStatusCode()!= HttpStatus.SC_OK){
                logger.error("Problem registering controls: " + response.getStatusLine());
                throw new IOException("Problem registering controls " + response.getStatusLine());
            }
        }
        finally {
            try {
                if (response != null) response.close();
            } catch (IOException e) {}
        }
    }

    public void postEvents(List<? extends Event> events) {
        CloseableHttpResponse response = null;
        String eventsPath = FeatureflowConfig.EVENTS_REST_PATH;
        Type type = new TypeToken<List<Event>>() {}.getType();
        String json = gson.toJson(events, type);
        HttpPost request = postRequest(apiKey, eventsPath, json);
        StringEntity entity = new StringEntity(json, UTF_8);
        entity.setContentType(APPLICATION_JSON);
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

    /*public void postFeatureEvalEvents(List<FeatureEvalEvent> featureEvalEvents) {
        CloseableHttpResponse response = null;
        String eventsPath = FeatureflowConfig.EVENTS_REST_PATH;
        Type type = new TypeToken<List<FeatureEvalEvent>>() {}.getType();
        String json = gson.toJson(featureEvalEvents, type);
        HttpPost request = postRequest(apiKey, eventsPath, json);
        StringEntity entity = new StringEntity(json, UTF_8);
        entity.setContentType(APPLICATION_JSON);
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
    }*/
   /* private void handleStatusCode(int status, String featureKey) throws IOException {

        if (status != HttpStatus.SC_OK) {
            if (status == HttpStatus.SC_UNAUTHORIZED) {
                logger.error("Unauthorized. Invalid API key");
            } else if (status == HttpStatus.SC_NOT_FOUND) {
                if (featureKey != null) {
                    logger.error("Unknown feature key: " + featureKey);
                }
                else {
                    logger.error("Not found");
                }
            } else {
                logger.error("Unexpected status code: " + status);
            }
            logger.error("Failed to fetch feature controls {} ", status);
            throw new IOException("Failed to fetch feature control " + status);
        }

    }*/
    private HttpPut putRequest(String apiKey, String path, String data) {
        URIBuilder builder = this.getBuilder().setPath(path);

        try {
            HttpPut request = new HttpPut(builder.build());
            StringEntity params =new StringEntity(data,"UTF-8");
            params.setContentType("application/json");
            request.addHeader("content-type", "application/json");
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
            request.addHeader("Accept-Language", "en-US,en;q=0.8");
            request.setEntity(params);

            request.addHeader("Authorization", "Bearer " + apiKey);
            request.addHeader("User-Agent", "JavaClient/" + VERSION);
            request.addHeader("X-Featureflow-Client", "JavaClient/" + VERSION); //We use this as we can pass it alongside default browser agent headers in JS

            return request;
        } catch (Exception e) {
            logger.error("Problem in PUT request ", e);
            return null;
        }
    }

    private HttpPost postRequest(String apiKey, String path, String data) {
        URIBuilder builder = this.getBuilder().setPath(path);
        try {
            HttpPost request = new HttpPost(builder.build());
            StringEntity params =new StringEntity(data,"UTF-8");
            params.setContentType("application/json");
            request.addHeader("content-type", "application/json");
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
            request.addHeader("Accept-Language", "en-US,en;q=0.8");
            request.setEntity(params);

            request.addHeader("Authorization", "Bearer " + apiKey);
            request.addHeader("User-Agent", "JavaClient/" + VERSION);

            return request;
        } catch (Exception e) {
            logger.error("Problem in POST request ", e);
            return null;
        }
    }

    private URIBuilder getBuilder() {
        URI base = URI.create(config.getBaseUri());
        return new URIBuilder()
                .setScheme(base.getScheme())
                .setHost(base.getHost())
                .setPort(base.getPort());
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

        ServiceUnavailableRetryStrategy unavailableRetryStrategy = new ServiceUnavailableRetryStrategy() {
            @Override
            public boolean retryRequest(
                    final HttpResponse response, final int executionCount, final HttpContext context) {
                int statusCode = response.getStatusLine().getStatusCode();
                return statusCode == 502 && executionCount < 1000;
            }

            @Override
            public long getRetryInterval() {
                return 5000;
            }
        };
        HttpRequestRetryHandler myRetryHandler = (exception, executionCount, context) -> {
            if (executionCount >= 5) {
                // Do not retry if over max retry count
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
            if (exception instanceof ConnectTimeoutException) {
                // Connection refused
                return false;
            }
            if (exception instanceof SSLException) {
                // SSL handshake exception
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                // Retry if the request is considered idempotent
                return true;
            }
            return false;
        };

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.connectTimeout)
                .setSocketTimeout(config.socketTimeout)
                .setProxy(config.getHttpProxyHost())
                .build();
        client = CachingHttpClients.custom()
                .setCacheConfig(cacheConfig)
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(myRetryHandler)
                .setServiceUnavailableRetryStrategy(unavailableRetryStrategy)
                .build();
        return client;
    }


}
