package io.featureflow.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.featureflow.client.core.EventSource;
import io.featureflow.client.core.EventSourceHandler;
import io.featureflow.client.core.MessageEvent;
import io.featureflow.client.model.FeatureControl;
import okhttp3.Headers;
import org.apache.http.concurrent.BasicFuture;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

/**
 * Created by oliver on 26/05/2016.
 */
public class FeatureFlowControlStreamTestInt {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlowControlStreamTestInt.class);
    private AtomicBoolean initialized = new AtomicBoolean(false);
    private EventSource eventSource; //   from https://github.com/aslakhellesoy/eventsource-java/blob/master/src/main/java/com/github/eventsource/client/EventSource.java

    @Test
    public void testStream() throws Exception {
        //Call startup and then wait for connection
        Future<Void> init = initStream();
        try {
            init.get(20000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("Timeout encountered waiting for Featureflow client initialise");
        } catch (Exception e) {
            logger.error("Exception encountered waiting for Featureflow client to initialise", e);
        }

        //ok we have a connection, lest wait for some onMessage


    }

    public Future<Void> initStream() throws URISyntaxException {
        BasicFuture startupFuture = new BasicFuture(null);
        EventSourceHandler handler = new EventSourceHandler() {
            @Override
            public void onConnect() throws Exception {
                logger.info("Featureflow Stream Connected");
                startupFuture.completed(null);
            }

            @Override
            public void onMessage(String name, MessageEvent event) throws Exception {
                Gson gson = new Gson();

                if(event.getData().startsWith("{\"heartbeat\"")){
                    logger.info("Heartbeat.");
                    return;
                }

                Type type = new TypeToken<Map<String, FeatureControl>>() {}.getType();

                Map<String, FeatureControl> controls = gson.fromJson(event.getData(), type);
                //store.init(features);

                for(Map.Entry<String, FeatureControl> entry : controls.entrySet()) {
                    logger.info("Received Message to update feature {} with {}.", entry.getKey(), entry.getValue());
                }
                if (!initialized.getAndSet(true)) {
                    startupFuture.completed(null);
                    logger.info("Featureflow client initialised.");
                }
            }
            @Override
            public void onError(Throwable throwable) {
                logger.warn("Error", throwable);
            }
        };

        Headers headers = new Headers.Builder()
                .add("Authorization", "Bearer " + "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4MzFiZThhZmQxODgzZDg4ZTQzMWEiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.1EvoDmtqOaAfYTtB3B1q7kSMp_Y27kQAa8GKM3fdHZcr1s6BQXHPW88U1j1K3Gwd4f0pHfZnSEJyZL0bd8kriA")
                .add("User-Agent", "FeatureflowClient-Java/" + "1.0")
                .add("Accept", "text/event-stream")
                .build();
        eventSource = new EventSource(
                new URI("http://localhost:8081/api/js/v1/controls/stream"),
                5000l,
                headers,
                handler);

        eventSource.init();

        return startupFuture;
    }



}