package org.jboss.aerogear.sync;

public class StandaloneConfig {

    // Diffsync server properties
    private final String host;
    private final int port;

    // Google Cloud Messaging properties
    private final boolean gcmEnabled;
    private final String gcmHost;
    private final int gcmPort;
    private final long gcmSenderId;
    private final String gcmApiKey;

    private StandaloneConfig(final Builder builder) {
        host = builder.host;
        port = builder.port;
        gcmEnabled = builder.gcmEnabled;
        gcmHost = builder.gcmHost;
        gcmPort = builder.gcmPort;
        gcmSenderId = builder.senderId;
        gcmApiKey = builder.apiKey;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String gcmHost() {
        return gcmHost;
    }

    public int gcmPort() {
        return gcmPort;
    }

    public long gcmSenderId() {
        return gcmSenderId;
    }

    public String gcmApiKey() {
        return gcmApiKey;
    }

    public boolean isGcmEnabled() {
        return gcmEnabled;
    }

    @Override
    public String toString() {
        return "StandaloneConfig[host=" + host +
                ", port=" + port +
                ", gcmEnabled=" + gcmEnabled +
                ", gcmHost=" + gcmHost +
                ", gcmPort=" + gcmPort +
                ", gcmSenderId=" + gcmSenderId +
                ", gcmApiKey=" + gcmApiKey + ']';
    }

    public static Builder host(final String host) {
        return new Builder(host);
    }

    public static class Builder {
        private final String host;
        private int port;
        private boolean gcmEnabled;
        private String gcmHost = "gcm.googleapis.com";
        private int gcmPort = 5235;
        private long senderId;
        private String apiKey;

        public Builder(final String host) {
            this.host = host;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder gcmEnabled() {
            gcmEnabled = true;
            return this;
        }

        public Builder gcmHost(final String gcmHost) {
            this.gcmHost = gcmHost;
            return this;
        }

        public Builder gcmPort(final int gcmPort) {
            this.gcmPort = gcmPort;
            return this;
        }

        public Builder gcmSenderId(final long senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder gcmApiKey(final String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public StandaloneConfig build() {
            return new StandaloneConfig(this);
        }

    }
}
