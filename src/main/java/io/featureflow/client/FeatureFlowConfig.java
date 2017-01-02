package io.featureflow.client;

import org.apache.http.HttpHost;

import java.net.URI;

/**
 * Created by oliver on 23/05/2016.
 */
public class FeatureFlowConfig {

    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 20000;

    public static final String DEFAULT_BASE_URI = "https://featureflow.io";
    private static final String DEFAULT_CONTROL_STREAM_PATH = "/api/app/v1/stream";
    static final String FEATURE_CONTROL_REST_PATH = "/api/app/v1/feature-controls";
    static final String REGISTER_REST_PATH = "/api/app/v1/register";

    private boolean offline = false;
    String proxyHost = null;
    String proxyScheme = null;
    int proxyPort = -1;
    int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    
    String baseURI = DEFAULT_BASE_URI;
    String controlStreamPath = DEFAULT_CONTROL_STREAM_PATH;


    FeatureFlowConfig(boolean offline, String proxyHost, String proxyScheme, int proxyPort, int connectTimeout, int socketTimeout, String baseURI) {
        this.offline = offline;
        this.proxyHost = proxyHost;
        this.proxyScheme = proxyScheme;
        this.proxyPort = proxyPort;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.baseURI = baseURI==null?DEFAULT_BASE_URI:baseURI;
    }

    HttpHost getHttpProxyHost() {
        if (this.proxyHost == null && this.proxyPort == -1 && this.proxyScheme == null) {
            return null;
        } else {
            String hostname = this.proxyHost == null ? "localhost" : this.proxyHost;
            String scheme = this.proxyScheme == null ? "https" : this.proxyScheme;
            return new HttpHost(hostname, this.proxyPort, scheme);
        }
    }

    boolean isOffline() {
        return offline;
    }

    void setOffline(boolean offline) {
        this.offline = offline;
    }

    String getProxyHost() {
        return proxyHost;
    }

    void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    String getProxyScheme() {
        return proxyScheme;
    }

    void setProxyScheme(String proxyScheme) {
        this.proxyScheme = proxyScheme;
    }

    int getProxyPort() {
        return proxyPort;
    }

    void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    int getConnectTimeout() {
        return connectTimeout;
    }

    void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    int getSocketTimeout() {
        return socketTimeout;
    }

    void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    String getBaseURI() {
        return baseURI==null?DEFAULT_BASE_URI:baseURI;
    }

    URI getControlStreamUri() {
        return controlStreamPath==null?URI.create(getBaseURI() + DEFAULT_CONTROL_STREAM_PATH):URI.create(baseURI+controlStreamPath);
    }



    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {
        private boolean offline = false;
        private String proxyHost = null;
        private String proxyScheme= null;
        private int proxyPort = -1;
        private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        private String baseURI = DEFAULT_BASE_URI;

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

        public Builder withBaseURI(String baseURI) {
            this.baseURI = baseURI;
            return this;
        }

        public FeatureFlowConfig build() {

            return new FeatureFlowConfig(offline, proxyHost, proxyScheme, proxyPort, connectTimeout, socketTimeout, baseURI);
        }

    }
     
}
