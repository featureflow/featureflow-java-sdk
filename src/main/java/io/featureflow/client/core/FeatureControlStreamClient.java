package io.featureflow.client.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.featureflow.client.FeatureControlCallbackHandler;
import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.model.FeatureControl;
import okhttp3.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oliver on 26/05/2016.
 */
public class FeatureControlStreamClient implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(FeatureControlStreamClient.class);
    public static final String FEATURES_UPDATED = "features.updated";
    public static final String FEATURES_DELETED = "features.deleted";
    private final FeatureflowConfig config;
    private final FeatureControlCache repository;
    private final Map<CallbackEvent, List<FeatureControlCallbackHandler>> callbacks;
    private EventSource eventSource; //   from https://github.com/aslakhellesoy/eventsource-java/blob/master/src/main/java/com/github/eventsource/client/EventSource.java
    Type mapOfFeatureControlsType = new TypeToken<Map<String, FeatureControl>>() {}.getType();
    Type listOfStringType  = new TypeToken<List<String>>() {}.getType();

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private String apiKey;

    /**
     * @param apiKey     The featureflow api key (channel id)
     * @param config     Some config
     * @param repository The feature Control Repository
     * @param callbacks A Map of event, List of  callback implementation for feature control events
     */
    public FeatureControlStreamClient(String apiKey,
                                      FeatureflowConfig config,
                                      FeatureControlCache repository,
                                      Map<CallbackEvent, List<FeatureControlCallbackHandler>> callbacks) {
        this.apiKey = apiKey;
        this.config = config;
        this.repository = repository;
        this.callbacks = callbacks;
    }
    //GET Controls form /api/sdk/v1/controls/stream
    public Future<Void> start() {
        final NoOpFuture initFuture = new NoOpFuture();
        //2.else load all feature controls from ff
        Headers headers = new Headers.Builder()
                .add("Authorization", "Bearer " + this.apiKey)
                .add("User-Agent", "FeatureflowClient-Java/" + "1.0")
                .add("Cache-Control", "no-cache")
                //.add("Accept", "text/event-stream")
                .build();
        eventSource = new EventSource(config.getControlStreamUri(), 5000l, headers, getHandler(initFuture));
        eventSource.init();
        return initFuture;
    }

    private EventSourceHandler getHandler(NoOpFuture initFuture){
        EventSourceHandler handler = new EventSourceHandler() {
            @Override
            public void onConnect() throws Exception {
                logger.info("Featureflow Connected");
            }

            @Override
            public void onMessage(String name, EventSourceMessage event) throws Exception {
                Gson gson = new Gson();
                if (event.getData().startsWith("{\"heartbeat\"")) {
                    if (!initialized.getAndSet(true)) {
                        initFuture.completed(null);
                        logger.info("Featureflow client inititalised.");
                    }
                    return;
                }
                if(FEATURES_DELETED.equals(name)){
                    List<String> featureKeys = gson.fromJson(event.getData(), listOfStringType);
                    for (String featureKey : featureKeys) {
                        FeatureControl deletedControl = repository.get(featureKey);
                        if (logger.isDebugEnabled())
                            logger.debug("Received Message to delete feature {}.", featureKey);
                        repository.delete(featureKey);
                        //invoking callbacks
                        if (callbacks != null && callbacks.get(CallbackEvent.DELETED_FEATURE)!=null){
                            for (FeatureControlCallbackHandler callback: callbacks.get(CallbackEvent.DELETED_FEATURE)) {
                                callback.onUpdate(deletedControl);
                            }
                        }
                    }
                }
                if(FEATURES_UPDATED.equals(name)){
                    Map<String, FeatureControl> controls = gson.fromJson(event.getData(), mapOfFeatureControlsType);
                    for (Map.Entry<String, FeatureControl> entry : controls.entrySet()) {
                        if (logger.isDebugEnabled())
                            logger.debug("Received Message to update feature {} enabled: {}.", entry.getKey(), entry.getValue().enabled);
                        repository.update(entry.getKey(), entry.getValue());
                        if (callbacks != null && callbacks.get(CallbackEvent.UPDATED_FEATURE)!=null){
                            for (FeatureControlCallbackHandler callback: callbacks.get(CallbackEvent.UPDATED_FEATURE)) {
                                callback.onUpdate(entry.getValue());
                            }
                        }
                    }
                }

                if (!initialized.getAndSet(true)) {
                    initFuture.completed(null);
                    logger.info("Featureflow client initialised.");
                }
            }

            @Override
            public void onError(Throwable throwable) {
                logger.warn("Error", throwable);
            }
        };
        return handler;
    }

    @Override
    public void close() throws IOException {
        if (eventSource != null) {
            eventSource.close();
        }
        if (repository != null) {
            repository.close();
        }
    }

    // @Override
    public boolean initialized() {
        return initialized.get();
    }

}