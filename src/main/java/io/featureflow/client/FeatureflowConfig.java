package io.featureflow.client;

import org.apache.hc.core5.http.HttpHost;

/**
 * Updated to use HttpClient 5.x
 */
public class FeatureflowConfig {

    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 20000;

    public static final String DEFAULT_FEATURE_EVENT_URI = "https://events.featureflow.io/api/sdk/v1/events"; //The Feature Event URL - eg https://events.featureflow.io/api/sdk/v1/events"
    public static final String DEFAULT_REGISTER_FEATURE_URI = "https://events.featureflow.io/api/sdk/v1/register"; //The Register URL - eg https://events.featureflow.io/api/sdk/v1/register
    private static final String DEFAULT_STREAM_URI = "https://rtm.featureflow.io/api/sdk/v1/features"; //The SSE Stream Base URL - eg https://rtm.featureflow.io/api/sdk/v1/features
    public static final String VERSION = "1.2.0";


    private boolean offline;
    private String proxyHost;
    private String proxyScheme;
    private int proxyPort;
    private int connectTimeout;
    private int socketTimeout;
    private String featureEventUri;
    private String registerFeatureUri;
    private String streamUri;


    public long waitForStartup = 10000l;

    FeatureflowConfig(String proxyHost, String proxyScheme, int proxyPort, int connectTimeout, int socketTimeout, String featureEventUri, String registerFeatureUri, String streamUri, long waitForStartup, boolean offline) {
        this.proxyHost = proxyHost;
        this.proxyScheme = proxyScheme;
        this.proxyPort = proxyPort;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.featureEventUri = featureEventUri;
        this.registerFeatureUri = registerFeatureUri;
        this.streamUri = streamUri;

        this.waitForStartup = waitForStartup;
        this.offline = offline;
    }

    public static Builder builder() {
        return new Builder();
    }

    public HttpHost getHttpProxyHost() {
        if (this.proxyHost == null && this.proxyPort == -1 && this.proxyScheme == null) {
            return null;
        } else {
            String hostname = this.proxyHost == null ? "localhost" : this.proxyHost;
            String scheme = this.proxyScheme == null ? "https" : this.proxyScheme;
            return new HttpHost(scheme, hostname, this.proxyPort);
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

    public String getRegisterFeatureUri() {
        return registerFeatureUri;
    }
    public String getFeatureEventUri() {
        return featureEventUri;
    }

    public String getStreamUri() {
        return streamUri;
    }

    public long getWaitForStartup() {
        return waitForStartup;
    }



    public class Event {
        public int queueSize = 10000;
    }


    public static class Builder {
        private String proxyHost = null;
        private String proxyScheme = null;
        private int proxyPort = -1;
        private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

        private String featureEventUri = DEFAULT_FEATURE_EVENT_URI;
        private String registerFeatureUri = DEFAULT_REGISTER_FEATURE_URI;
        private String streamUri = DEFAULT_STREAM_URI;

        long waitForStartup = 10000;
        boolean offline = false;

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

        public Builder withFeatureEventUri(String featureEventUri) {
            this.featureEventUri = featureEventUri;
            return this;
        }

        public Builder withRegisterFeatureUri(String registerFeatureUri) {
            this.registerFeatureUri = registerFeatureUri;
            return this;
        }

        public Builder withStreamUri(String streamUri) {
            this.streamUri = streamUri;
            return this;
        }

        public Builder withWaitForStartup(long waitTimeMilliseconds) {
            this.waitForStartup = waitTimeMilliseconds;
            return this;
        }

        public Builder withOffline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public FeatureflowConfig build() {
            return new FeatureflowConfig(proxyHost, proxyScheme, proxyPort, connectTimeout, socketTimeout, featureEventUri, registerFeatureUri, streamUri, waitForStartup, offline);
        }
    }

}
