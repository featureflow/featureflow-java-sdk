package io.featureflow.client;

import org.apache.http.HttpHost;

import java.net.URI;

/**
 * Created by oliver on 23/05/2016.
 */
public class FeatureflowConfig {

    private static final int DEFAULT_CONNECT_TIMEOUT        = 30000;
    private static final int DEFAULT_SOCKET_TIMEOUT         = 20000;
    private static final String DEFAULT_STREAM_BASE_URI        = "https://rtm.featureflow.io"; //The SSE Stream Base URL - eg https://rtm.featureflow.io/api/sdk/v1/features
    private static final String DEFAULT_SDK_BASE_URL           = "https://app.featureflow.io"; //The REST backup polling URL - eg https://sdk.featureflow.io/api/sdk/v1/features
    public static final String DEFAULT_EVENTS_BASE_URI         = "https://events.featureflow.io"; //POST Events URL - eg https://events.featureflow.io/api/sdk/v1/events https://events.featureflow.io/api/sdk/v1/register

    public static final String FEATURES_STREAM_PATH = "/api/sdk/v1/features";
    public static final String FEATURES_REST_PATH   = "/api/sdk/v1/features";
    public static final String REGISTER_REST_PATH        = "/api/sdk/v1/register";
    public static final String EVENTS_REST_PATH          = "/api/sdk/v1/events";

    public static final String VERSION                      = "1.0.4";


    public boolean offline      = false;
    public String proxyHost     = null;
    public String proxyScheme   = null;
    public int proxyPort        = -1;
    public int connectTimeout   = DEFAULT_CONNECT_TIMEOUT;
    public int socketTimeout    = DEFAULT_SOCKET_TIMEOUT;


    public String sdkBaseUri    = DEFAULT_SDK_BASE_URL;
    public String eventsBaseUri = DEFAULT_EVENTS_BASE_URI;
    public String streamBaseUri = DEFAULT_STREAM_BASE_URI;


    public long waitForStartup  = 10000l;

    FeatureflowConfig(String proxyHost, String proxyScheme, int proxyPort, int connectTimeout, int socketTimeout, String sdkBaseUri, String streamBaseUri, String eventsBaseUri, long waitForStartup) {
        this.proxyHost = proxyHost;
        this.proxyScheme = proxyScheme;
        this.proxyPort = proxyPort;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.sdkBaseUri = sdkBaseUri;
        this.streamBaseUri = streamBaseUri;
        this.eventsBaseUri = eventsBaseUri;

        this.waitForStartup = waitForStartup;
    }
    public static Builder builder(){
        return new Builder();
    }

    public HttpHost getHttpProxyHost() {
        if (this.proxyHost == null && this.proxyPort == -1 && this.proxyScheme == null) {
            return null;
        } else {
            String hostname = this.proxyHost == null ? "localhost" : this.proxyHost;
            String scheme = this.proxyScheme == null ? "https" : this.proxyScheme;
            return new HttpHost(hostname, this.proxyPort, scheme);
        }
    }

    public boolean isOffline() {
        return offline;
    }
    public String getProxyHost() {
        return proxyHost;
    }
    public String getProxyScheme() {
        return proxyScheme;
    }
    public int getProxyPort() {
        return proxyPort;
    }
    public int getConnectTimeout() {
        return connectTimeout;
    }
    public int getSocketTimeout() {
        return socketTimeout;
    }

    public String getSdkBaseUri() {return sdkBaseUri;}
    public String getEventBaseUri() {
        return eventsBaseUri;
    }
    public String getStreamBaseUri() {return streamBaseUri;}
    public long getWaitForStartup(){
        return waitForStartup;
    }
    public class Event {
        public int queueSize = 10000;
    }


    public static class Builder {
        private String proxyHost = null;
        private String proxyScheme= null;
        private int proxyPort = -1;
        private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

        private String sdkBaseURI = DEFAULT_SDK_BASE_URL;
        private String streamBaseUri = DEFAULT_STREAM_BASE_URI;
        private String eventsBaseUri = DEFAULT_EVENTS_BASE_URI;

        long waitForStartup = 10000;

        public Builder withProxyHost(String proxyHost) {
            this.proxyHost = proxyHost;
            return this;
        }

        public Builder withProxyScheme(String proxyScheme) {
            this.proxyScheme = proxyScheme;
            return this;
        }

        public Builder withProxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public Builder withConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder withSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        @Deprecated//use withSdkBaseUri
        public Builder withBaseUri(String baseUri) {
            this.sdkBaseURI = baseUri;
            return this;
        }
        public Builder withSdkBaseUri(String sdkBaseURI) {
            this.sdkBaseURI = sdkBaseURI;
            return this;
        }
        public Builder withStreamBaseUri(String streamBaseUri) {
            this.streamBaseUri = streamBaseUri;
            return this;
        }
        public Builder withWaitForStartup(long waitTimeMilliseconds){
            this.waitForStartup = waitTimeMilliseconds;
            return this;
        }
        public FeatureflowConfig build() {
            return new FeatureflowConfig(proxyHost, proxyScheme, proxyPort, connectTimeout, socketTimeout, sdkBaseURI, streamBaseUri, eventsBaseUri, waitForStartup);
        }

    }
     
}
