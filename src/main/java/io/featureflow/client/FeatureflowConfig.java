package io.featureflow.client;

import org.apache.http.HttpHost;

import java.net.URI;

/**
 * Created by oliver on 23/05/2016.
 */
public class FeatureflowConfig {

    private static final int DEFAULT_CONNECT_TIMEOUT        = 30000;
    private static final int DEFAULT_SOCKET_TIMEOUT         = 20000;
    public static final String DEFAULT_BASE_URI             = "https://app.featureflow.io";
    public static final String DEFAULT_STREAM_BASE_URI             = "https://rtm.featureflow.io";
    //private static final String DEFAULT_CONTROL_STREAM_PATH = "/api/sdk/v1/stream";
    private static final String DEFAULT_CONTROL_STREAM_PATH = "/api/sdk/v1/controls/stream";
    public static final String FEATURE_CONTROL_REST_PATH           = "/api/sdk/v1/feature-controls";
    public static final String REGISTER_REST_PATH                  = "/api/sdk/v1/register";
    public static final String EVENTS_REST_PATH                    = "/api/sdk/v1/events";

    public boolean offline = false;
    public String proxyHost = null;
    public String proxyScheme = null;
    public int proxyPort = -1;
    public int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    public int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    public String baseUri = DEFAULT_BASE_URI;
    public String streamBaseUri = DEFAULT_STREAM_BASE_URI;
    public String controlStreamPath = DEFAULT_CONTROL_STREAM_PATH;
    public long waitForStartup = 10000l;

    FeatureflowConfig(boolean offline, String proxyHost, String proxyScheme, int proxyPort, int connectTimeout, int socketTimeout, String baseURI, String streamBaseUri, long waitForStartup) {
        this.offline = offline;
        this.proxyHost = proxyHost;
        this.proxyScheme = proxyScheme;
        this.proxyPort = proxyPort;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.baseUri = baseURI==null?DEFAULT_BASE_URI:baseURI;
        this.streamBaseUri = streamBaseUri ==null?DEFAULT_STREAM_BASE_URI: streamBaseUri;
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
    public String getBaseUri() {return baseUri ==null?DEFAULT_BASE_URI: baseUri;}
    public String getStreamBaseUri() {
        return streamBaseUri ==null?DEFAULT_STREAM_BASE_URI: streamBaseUri;
    }
    public URI getControlStreamUri() {return controlStreamPath==null?URI.create(getStreamBaseUri() + DEFAULT_CONTROL_STREAM_PATH):URI.create(getStreamBaseUri()+controlStreamPath);}
    public long getWaitForStartup(){
        return waitForStartup;
    }
    public class Event {
        public int queueSize = 10000;
    }


    public static class Builder {
        private boolean offline = false;
        private String proxyHost = null;
        private String proxyScheme= null;
        private int proxyPort = -1;
        private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        private String baseURI = DEFAULT_BASE_URI;
        private String streamBaseUri = DEFAULT_STREAM_BASE_URI;
        long waitForStartup = 10000;

        public Builder withOffline(boolean offline) {
            this.offline = offline;
            return this;
        }

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

        public Builder withBaseUri(String baseUri) {
            this.baseURI = baseUri;
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
            return new FeatureflowConfig(offline, proxyHost, proxyScheme, proxyPort, connectTimeout, socketTimeout, baseURI, streamBaseUri, waitForStartup);
        }

    }
     
}
