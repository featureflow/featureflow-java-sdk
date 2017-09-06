package io.featureflow.client;



import com.google.gson.JsonPrimitive;
import io.featureflow.client.core.*;
import io.featureflow.client.core.CallbackEvent;
import io.featureflow.client.model.Event;
import io.featureflow.client.model.Feature;
import io.featureflow.client.model.FeatureControl;
import io.featureflow.client.model.Variant;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by oliver on 23/05/2016.
 * The featureflow client is the interface to clients using featureflow.
 * The client uses the SSE Stream, Rest Client and Repository to manage features
 * Configuration may be provided to register feature controls and capabilitites
 * Callbacks may be assigned to proved ondemand updates to feature control changes
 */
public class FeatureflowClient implements Closeable{
    private static final Logger logger = LoggerFactory.getLogger(FeatureflowClient.class);
    private final FeatureflowConfig config;
    private final FeatureControlStreamClient featureControlStreamClient; // manages pubsub events to update a feature control
    private final FeatureControlCache featureControlCache; //holds the featureControls
    private final RestClient restClient; //manages retrieving features and pushing updates
    private final EventsClient eventHandler;
    private final Map<String, Feature> featuresMap = new HashMap<>(); //this contains code registered features and failovers
    private final FeatureflowUserProvider userProvider;
    FeatureflowClient(String apiKey, List<Feature> features, FeatureflowConfig config, Map<CallbackEvent, List<FeatureControlCallbackHandler>> callbacks, FeatureflowUserProvider userProvider) {
        //set config, use a builder
        this.config = config;
        this.userProvider = userProvider;

        featureControlCache = new SimpleMemoryFeatureCache();
        restClient = new RestClient(apiKey, config);
        eventHandler = new EventsClient(config, restClient);
        //Actively defining registrations helps alert if features are available in an environment
        if(features !=null&& features.size()>0){
            for (Feature feature : features) {
                featuresMap.put(feature.key, feature);
            }
            try {
                restClient.registerFeatureControls(features);
            } catch (IOException e) {
                logger.error("Problem registering feature controls", e);
            }
        }

        featureControlStreamClient = new FeatureControlStreamClient(apiKey, config, featureControlCache, callbacks);
        Future<Void> startFuture = featureControlStreamClient.start();
        if (config.waitForStartup > 0L) {
            logger.info("Waiting for Featureflow to inititalise");
            try {
                startFuture.get(config.waitForStartup, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.error("Timeout waiting for Featureflow client initialise");
            } catch (Exception e) {
                logger.error("Exception waiting for Featureflow client to initialise", e);
            }
        }
    }

    /**
     * Evaluate with a specific user, this will override any userProvider or anonymous user
     * @param featureKey
     * @param user
     * @return
     */
    public Evaluate evaluate (String featureKey, FeatureflowUser user) {
        Evaluate e = new Evaluate(this, featureKey, user);
        return e;
    }


    /**
     * Evaluate with a given context - use public Evaluate evaluate (String featureKey, FeatureflowUser user)  instead
     */
    @Deprecated
    public Evaluate evaluate(String featureKey, FeatureflowContext featureflowContext) {
        Evaluate e = new Evaluate(this, featureKey, featureflowContext);
        return e;
    }


    public Evaluate evaluate(String featureKey) {
        //create an anonymous user
        FeatureflowUser user = new FeatureflowUser();
        return evaluate(featureKey, user);
    }

    public Map<String, String> evaluateAll(FeatureflowContext featureflowContext) {
        return evaluateAll(new FeatureflowUser(featureflowContext));
    }
    public Map<String, String> evaluateAll(FeatureflowUser user){
        Map<String, String> result = new HashMap<>();
        for(String s: featureControlCache.getAll().keySet()){
            result.put(s, eval(s, user));
        }
        return result;
    }



    private String eval(String featureKey, FeatureflowContext featureflowContext) {
        FeatureflowUser user = new FeatureflowUser(featureflowContext.getKey()).withBucketKey(featureflowContext.getBucketKey()).withAttributes(featureflowContext.getValues());
        return eval(featureKey, user);
    }
    private String eval(String featureKey, FeatureflowUser user) {

        String failoverVariant = (featuresMap.get(featureKey)!=null&&featuresMap.get(featureKey).failoverVariant!=null)?featuresMap.get(featureKey).failoverVariant: Variant.off;
        FeatureControl control = featureControlCache.get(featureKey);;
        if(!featureControlStreamClient.initialized()){
            logger.warn("FeatureFlow is not initialized yet.");
        }
        if(control == null){
            logger.warn("Control does not exist, returning failover variant of " + failoverVariant);
            return failoverVariant;
        }

        //add featureflow.context
        addAdditionalContext(user);


        String variant = control.evaluate(user);
        return variant;

    }

    private void addAdditionalContext(FeatureflowUser user) {
        user.getAttributes().put(FeatureflowUser.FEATUREFLOW_USER_ID, new JsonPrimitive(user.getId()));
        user.getSessionAttributes().put(FeatureflowUser.FEATUREFLOW_HOUROFDAY, new JsonPrimitive(LocalTime.now().getHour()));
        user.getSessionAttributes().put(FeatureflowUser.FEATUREFLOW_DATE, new JsonPrimitive(FeatureflowContext.toIso(new DateTime())));
    }
    public void close() throws IOException {
        this.eventHandler.close();
    }

    public static Builder builder(String apiKey){
        return new FeatureflowClient.Builder(apiKey);
    }

    public void track(String goalKey, FeatureflowUser user) {
        eventHandler.queueEvent(new Event(goalKey, user, evaluateAll(user)));

    }

    public static class Builder {
        private FeatureflowConfig config = null;
        private String apiKey;
        private Map<CallbackEvent, List<FeatureControlCallbackHandler>> featureControlCallbackHandlers = new HashMap<>();
        private FeatureflowUserProvider userProvider;
        private List<Feature> features = new ArrayList<>();

        public Builder (String apiKey){
            this.apiKey = apiKey;
        }

        public Builder withUpdateCallback(FeatureControlCallbackHandler featureControlCallbackHandler){
            this.withCallback(CallbackEvent.UPDATED_FEATURE, featureControlCallbackHandler);
            return this;
        }
        public Builder withDeleteCallback(FeatureControlCallbackHandler featureControlCallbackHandler){
            this.withCallback(CallbackEvent.DELETED_FEATURE, featureControlCallbackHandler);
            return this;
        }


        public Builder withCallback(CallbackEvent event, FeatureControlCallbackHandler featureControlCallbackHandler){
            if(featureControlCallbackHandlers.get(event)==null){
                featureControlCallbackHandlers.put(event, new ArrayList<>());
            }
            this.featureControlCallbackHandlers.get(event).add(featureControlCallbackHandler);
            return this;
        }

        public Builder withUserProvider(FeatureflowUserProvider userProvider){
            this.userProvider = userProvider;
            return this;
        }
        public Builder withConfig(FeatureflowConfig config){
            this.config = config;
            return this;
        }

        public Builder withFeature(Feature feature){
            this.features.add(feature);
            return this;
        }
        public Builder withFeatures(List<Feature> features){
            this.features = features;
            return this;
        }

        public FeatureflowClient build(){
            if(config==null){ config = new FeatureflowConfig.Builder().build();}
            return new FeatureflowClient(apiKey, features, config, featureControlCallbackHandlers, userProvider);
        }
    }

    public class Evaluate {
        private final String evaluateResult;
        private final String featureKey;
        private final FeatureflowUser user;

        Evaluate(FeatureflowClient featureflowClient, String featureKey, FeatureflowContext featureflowContext) {
            this.featureKey = featureKey;
            this.user  = new FeatureflowUser(featureflowContext);
            this.evaluateResult = featureflowClient.eval(featureKey, user);
        }
        Evaluate(FeatureflowClient featureflowClient, String featureKey, FeatureflowUser user) {
            this.featureKey = featureKey;
            this.user = user;
            this.evaluateResult = featureflowClient.eval(featureKey, user);
        }

        Evaluate(FeatureflowClient featureflowClient, String featureKey, String userId) {
            this.featureKey = featureKey;
            this.user = userProvider!=null?userProvider.getUser(userId):new FeatureflowUser(userId);
            this.evaluateResult = featureflowClient.eval(featureKey, user);
        }
        public boolean isOn(){
            return is(Variant.on);
        }
        public boolean isOff(){
            return is(Variant.off);
        }
        public boolean is(String variant){
            eventHandler.queueEvent(new Event(featureKey, Event.EVALUATE_EVENT, user, evaluateResult, variant));
            return variant.equals(evaluateResult);
        }
        public String value(){
            eventHandler.queueEvent(new Event(featureKey, Event.EVALUATE_EVENT, user, evaluateResult, null));
            return evaluateResult;
        }
    }
}
