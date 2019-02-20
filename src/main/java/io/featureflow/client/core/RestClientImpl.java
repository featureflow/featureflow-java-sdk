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
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
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
public class RestClientImpl implements RestClient {


    private static final String APPLICATION_JSON = "application/json";
    private static final String UTF_8 = "UTF-8";

    private final String apiKey;
    private final FeatureflowConfig config;
    private CloseableHttpClient client = null;

    private Gson gson =  new GsonBuilder()
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
        HttpCacheContext context = HttpCacheContext.create();
        HttpPut request = putFeaturesRequest(apiKey, gson.toJson(featureRegistrations));
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
    @Override
    public void postEvents(List<? extends Event> events) {
        CloseableHttpResponse response = null;
        Type type = new TypeToken<List<Event>>() {}.getType();
        String json = gson.toJson(events, type);
        HttpPost request = postRequest(apiKey, "/api/sdk/v1/events", json);
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

    private HttpPut putFeaturesRequest(String apiKey, String data) {
        URI path =  URI.create(config.getSdkBaseUri() + "/api/sdk/v1/register");
        try {
            HttpPut request = new HttpPut(path);
            setRequestVals(request, data);
            return request;
        } catch (Exception e) {
            logger.error("Problem in PUT request ", e);
            return null;
        }
    }

    private HttpPost postRequest(String apiKey, String path, String data) {
        URI base = URI.create(config.getEventBaseUri());

        try {
            URI uri = new URIBuilder()
                    .setScheme(base.getScheme())
                    .setHost(base.getHost())
                    .setPort(base.getPort())
                    .setPath(path).build();

            HttpPost request = new HttpPost(uri);
            setRequestVals(request, data);
            return request;
        } catch (Exception e) {
            logger.error("Problem in POST request ", e);
            return null;
        }
    }

    private void setRequestVals(HttpEntityEnclosingRequestBase request, String data){
        StringEntity params =new StringEntity(data,"UTF-8");
        params.setContentType("application/json");
        request.addHeader("content-type", "application/json");
        request.addHeader("Accept", "*/*");
        request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
        request.addHeader("Accept-Language", "en-US,en;q=0.8");
        request.setEntity(params);

        request.addHeader("Authorization", "Bearer " + apiKey);
        request.addHeader("X-Featureflow-Client", "JavaClient/" + FeatureflowConfig.VERSION);

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
                .setConnectTimeout(config.getConnectTimeout())
                .setSocketTimeout(config.getSocketTimeout())
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
