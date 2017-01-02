package io.featureflow.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oliver on 26/05/2016.
 */
public class FeatureControlRestClient {
    public static final String VERSION = "0.0.1";
    private final String apiKey;
    private final FeatureFlowConfig config;
    private CloseableHttpClient client = null;
    private static final Logger logger = LoggerFactory.getLogger(FeatureControlRestClient.class);

    public FeatureControlRestClient(String apiKey, FeatureFlowConfig config) {
        this.apiKey = apiKey;
        this.config = config;

    }


   /* public String registerFeatures(Map<String, FeatureRegistration> featureRegistrationMap){
        if (client==null)client = createHttpClient();
        logger.info("Loading feature controls");
        Gson gson = new Gson();
        HttpCacheContext context = HttpCacheContext.create();

        String resource = FeatureFlowConfig.DEFAULT_FEATURE_CONTROL_REST_PATH;

        HttpGet request = getRequest(apiKey, resource);

        CloseableHttpResponse response = null;
        try {
           ///..... REGISTER
        }
        finally {
            try {
                if (response != null) response.close();
            } catch (IOException e) {
            }
        }
    }*/

    /**
     * Register any code defined feature controls as available and retrieve a list of features form the server
     * @return
     * @throws IOException
     */
    public Map<String, FeatureControl> registerFeatureControls(Map<String, FeatureRegistration> featureRegistrationMap) throws IOException{
        logger.info("Registering feature controls");
        Gson gson = new Gson();
        HttpCacheContext context = HttpCacheContext.create();
        String resource = FeatureFlowConfig.REGISTER_REST_PATH;
        HttpPut request = putRequest(apiKey, resource, gson.toJson(featureRegistrationMap));
        CloseableHttpResponse response = null;
        try {
            logger.debug("Requesting: " + request);
            response = client.execute(request, context);
            handleStatusCode(response.getStatusLine().getStatusCode(), null);
            //Type type = new TypeToken<Map<String, FeatureControl>>() {}.getType();
            Type type = new TypeToken<List<FeatureControl>>() {}.getType();
            String json = EntityUtils.toString(response.getEntity());
            logger.debug("Response: " + response.toString());
            logger.debug("Response JSON: " + json);
            List<FeatureControl> result = gson.fromJson(json, type);
            return result.stream().collect(Collectors.toMap(FeatureControl::getKey, Function.identity()));
        }
        finally {
            try {
                if (response != null) response.close();
            } catch (IOException e) {}
        }
    }

    public Map<String, FeatureControl> getFeatureControls() throws IOException{
        if (client==null)client = createHttpClient();
        logger.info("Loading feature controls");
        Gson gson = new Gson();
        HttpCacheContext context = HttpCacheContext.create();

        String resource = FeatureFlowConfig.FEATURE_CONTROL_REST_PATH ;

        HttpGet request = getRequest(apiKey, resource);

        CloseableHttpResponse response = null;
        try {
            logger.debug("Requesting: " + request);
            response = client.execute(request, context);

            logCacheResponseStatus(context.getCacheResponseStatus());
            handleStatusCode(response.getStatusLine().getStatusCode(), null);

            //Type type = new TypeToken<Map<String, FeatureControl>>() {}.getType();
            Type type = new TypeToken<List<FeatureControl>>() {}.getType();
            String json = EntityUtils.toString(response.getEntity());
            logger.debug("Response: " + response.toString());
            logger.debug("Response JSON: " + json);
            List<FeatureControl> result = gson.fromJson(json, type);
            return result.stream().collect(Collectors.toMap(FeatureControl::getKey, Function.identity()));
        }
        finally {
            try {
                if (response != null) response.close();
            } catch (IOException e) {
            }
        }
    }

    private void logCacheResponseStatus(CacheResponseStatus status) {
        switch (status) {
            case CACHE_HIT:
                logger.debug("Cache Hit. A response was generated from the cache.");
                break;
            case CACHE_MODULE_RESPONSE:
                logger.debug("Cache Hit. The response was generated by the cache module");
                break;
            case CACHE_MISS:
                logger.debug("Cache Miss. The response came from an upstream server");
                break;
            case VALIDATED:
                logger.debug("Cache Partial Hit. The response was generated from the cache after validating with the FF server");
                break;
        }
    }

    private void handleStatusCode(int status, String featureKey) throws IOException {

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

    }

    private HttpGet getRequest(String apiKey, String path) {
        URIBuilder builder = this.getBuilder().setPath(path);

        try {
            HttpGet request = new HttpGet(builder.build());
            request.addHeader("Authorization", "Bearer " + apiKey);
            request.addHeader("User-Agent", "JavaClient/" + VERSION);

            return request;
        } catch (Exception e) {
            logger.error("Problem receiving feature repository from Featureflow servers ", e);
            return null;
        }
    }
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

            return request;
        } catch (Exception e) {
            logger.error("Problem in PUT request ", e);
            return null;
        }
    }
    /*private HttpPost postRequest(String apiKey, String path) {
        URIBuilder builder = this.getBuilder().setPath(path);

        try {
            HttpPost request = new HttpPost(builder.build());
            request.addHeader("Authorization", "Bearer " + apiKey);
            request.addHeader("User-Agent", "JavaClient/" + "0.0.1");

            return request;
        } catch (Exception e) {
            logger.error("Problem in POST request ", e);
            return null;
        }
    }*/

    private URIBuilder getBuilder() {
        URI base = URI.create(config.baseURI);
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
