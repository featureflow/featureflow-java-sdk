package io.featureflow.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oliver on 26/05/2016.
 */
public class FeatureControlStreamManager implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(FeatureControlStreamManager.class);
    private static final String PUT = "put";
    private static final String PATCH = "patch";
    private static final String DELETE = "delete";
    private final FeatureFlowConfig config;
    private final FeatureControlRepository repository;
    private final FeatureControlEventHandler callback;
    private EventSource eventSource; //   from https://github.com/aslakhellesoy/eventsource-java/blob/master/src/main/java/com/github/eventsource/client/EventSource.java

    private final FeatureControlRestClient featureControlRestClient;

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private String apiKey;

    /**
     * @param apiKey                   The featureflow api key (channel id)
     * @param config                   Some config
     * @param repository               The feature Control Repository
     * @param featureControlRestClient The rest client
     */
    public FeatureControlStreamManager(String apiKey,
                                       FeatureFlowConfig config,
                                       FeatureControlRepository repository,
                                       FeatureControlRestClient featureControlRestClient,
                                       FeatureControlEventHandler callback) {
        this.apiKey = apiKey;
        this.config = config;
        this.repository = repository;
        this.featureControlRestClient = featureControlRestClient;
        this.callback = callback;
    }


    //@Override
    public Future<Void> start() {
        final NoOpFuture initFuture = new NoOpFuture();


        //inititalise the repository
        //1. check if there is a cluster and join


            //2.else load all feature controls from ff
            //We do this in a separate thread - the app will continue to start up - in featureflow this allows featureflow to start so it can serve itself its features :S
            CompletableFuture initF = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            Map<String, FeatureControl> featureControlMap = featureControlRestClient.getFeatureControls();
                            //3. init repo
                            repository.init(featureControlMap);
                            //4. Subscribe to feature events feed
                            //subscribeToEvents();
                            Headers headers = new Headers.Builder()
                                    .add("Authorization", "Bearer " + this.apiKey)
                                    .add("User-Agent", "FeatureflowClient-Java/" + "1.0")
                                    .add("Accept", "text/event-stream")
                                    .build();

                            EventSourceHandler handler = new EventSourceHandler() {

                                @Override
                                public void onConnect() throws Exception {
                                    logger.info("Featureflow Connected");
                                }

                                @Override
                                public void onMessage(String name, MessageEvent event) throws Exception {
                                    Gson gson = new Gson();

                                    if(event.getData().startsWith("{\"heartbeat\"")){
                                        if (!initialized.getAndSet(true)) {
                                            initFuture.completed(null);
                                            logger.info("Featureflow client inititalised.");
                                        }
                                        return;
                                    }

                                    Type type = new TypeToken<Map<String, FeatureControl>>() {}.getType();

                                    Map<String, FeatureControl> controls = gson.fromJson(event.getData(), type);
                                    //store.init(features);


                                    for(Map.Entry<String, FeatureControl> entry : controls.entrySet()) {
                                        logger.info("Received Message to update feature {} with {}.", entry.getKey(), entry.getValue().enabled);
                                        repository.update(entry.getKey(), entry.getValue());
                                        //invoking callbacks
                                        if(callback!=null)callback.onUpdate(entry.getValue());
                                    }

                                    //repository.get()update(controls.get());
                                    if (!initialized.getAndSet(true)) {
                                        initFuture.completed(null);
                                        logger.info("Featureflow client inititalised.");
                                    }

                /*if (name.equals(PUT)) {
                    //Type type = new TypeToken<Map<String,FeatureRep<?>>>(){}.getType();
                    Type type = new TypeToken<Map<String, FeatureControl>>() {}.getType();
                    Map<String, FeatureControl> controls = gson.fromJson(event.getData(), type);
                    //store.init(features);
                    repository.init(controls);
                    if (!initialized.getAndSet(true)) {
                        initFuture.completed(null);
                        logger.info("Featureflow client inititalised.");
                    }
                }*/
                /*else if (name.equals(PATCH)) {
                    FeaturePatchData data = gson.fromJson(event.getData(), FeaturePatchData.class);
                    store.upsert(data.key(), data.feature());
                }
                else if (name.equals(DELETE)) {
                    FeatureDeleteData data = gson.fromJson(event.getData(), FeatureDeleteData.class);
                    store.delete(data.key(), data.version());
                }
                else if (name.equals(INDIRECT_PUT)) {
                    try {
                        Map<String, FeatureRep<?>> features = requestor.makeAllRequest(true);
                        store.init(features);
                        if (!initialized.getAndSet(true)) {
                            initFuture.completed(null);
                            logger.info("Initialized Featureflow client.");
                        }
                    } catch (IOException e) {
                        logger.error("Encountered exception in Featureflow client", e);
                    }
                }
                else if (name.equals(INDIRECT_PATCH)) {
                    String key = event.getData();
                    try {
                        FeatureRep<?> feature = requestor.makeRequest(key, true);
                        store.upsert(key, feature);
                    } catch (IOException e) {
                        logger.error("Encountered exception in Featureflow client", e);
                    }
                }*/
                /*else {
                    logger.warn("Unexpected event found in stream: " + event.getData());
                }*/
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    logger.warn("Error", throwable);
                                }
                            };

                            eventSource = new EventSource(config.getControlStreamUri(), 5000l, headers, handler);

                            eventSource.init();

                        } catch (IOException e) { }
                        return initFuture;
                    });

            return  initF;

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