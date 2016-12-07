package io.featureflow.client;

import okhttp3.Headers;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * Created by oliver on 25/05/2016.
 * http://grepcode.com/file_/repo1.maven.org/maven2/org.glassfish.jersey.media/jersey-media-sse/2.19/org/glassfish/jersey/media/sse/EventSource.java/?v=source
 * EventSource
 *
 *
 */
public class FeatureStreamManager implements StreamClient{
    private final FeatureFlowConfig config;
    private final FeatureControlRepository featureControlRepository;
    private final String apiKey;

    public FeatureStreamManager(String apiKey, FeatureFlowConfig config) {
        featureControlRepository = new SimpleMemoryFeatureRepository();
        this.apiKey = apiKey;
        this.config = config;
    }

    public Future<Void> start() {
        final BasicFuture initFuture = new BasicFuture(new FutureCallback<Void>() {
            public void completed(Void o) {}
            public void failed(Exception e) {}
            public void cancelled() {}
        });

        Headers headers = new Headers.Builder()
                .add("Authorization", "api_key " + this.apiKey)
                .add("User-Agent", "java/" + "1.0.0")
                .add("Accept", "text/event-stream")
                .build();



        return null;
    }

    public boolean initialized() {
        return false;
    }

    public void close() throws IOException {

    }


}
