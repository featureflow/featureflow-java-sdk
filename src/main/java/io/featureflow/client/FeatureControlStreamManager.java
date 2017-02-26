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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oliver on 26/05/2016.
 */
public class FeatureControlStreamManager implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(FeatureControlStreamManager.class);
    private final FeatureFlowConfig config;
    private final FeatureControlRepository repository;
    private final FeatureControlUpdateHandler callback;
    private EventSource eventSource; //   from https://github.com/aslakhellesoy/eventsource-java/blob/master/src/main/java/com/github/eventsource/client/EventSource.java

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private String apiKey;

    /**
     * @param apiKey                   The featureflow api key (channel id)
     * @param config                   Some config
     * @param repository               The feature Control Repository
     */
    public FeatureControlStreamManager(String apiKey,
                                       FeatureFlowConfig config,
                                       FeatureControlRepository repository,
                                       FeatureControlUpdateHandler callback) {
        this.apiKey = apiKey;
        this.config = config;
        this.repository = repository;
        this.callback = callback;
    }


    //@Override
    public Future<Void> start() {
        final NoOpFuture initFuture = new NoOpFuture();
            //2.else load all feature controls from ff
            //We do this in a separate thread - the app will continue to start up - in featureflow this allows featureflow to start so it can serve itself its features :S
           /* CompletableFuture initF = CompletableFuture.supplyAsync(
                    () -> {*/
                            Headers headers = new Headers.Builder()
                                    .add("Authorization", "Bearer " + this.apiKey)
                                    .add("User-Agent", "FeatureflowClient-Java/" + "1.0")
                                    .add("Cache-Control", "no-cache")
                                    //.add("Accept", "text/event-stream")
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
                                        if(logger.isDebugEnabled())logger.debug("Received Message to update feature {} with {}.", entry.getKey(), entry.getValue().enabled);
                                        repository.update(entry.getKey(), entry.getValue());
                                        //invoking callbacks
                                        if(callback!=null)callback.onUpdate(entry.getValue());
                                    }

                                    //repository.get()update(controls.get());
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

                            eventSource = new EventSource(config.getControlStreamUri(), 5000l, headers, handler);

                            eventSource.init();
                        return initFuture;
           /*         });

            return  initF;*/

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