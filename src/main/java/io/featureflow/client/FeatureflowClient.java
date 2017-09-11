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
    private final FeatureflowUserLookupProvider userLookupProvider;
    private final boolean offline;

    FeatureflowClient(
            String apiKey, List<Feature> features, FeatureflowConfig config, Map<CallbackEvent,
            List<FeatureControlCallbackHandler>> callbacks,
            FeatureflowUserProvider userProvider,
            FeatureflowUserLookupProvider userLookupProvider,
            boolean offline ) {
        //set config, use a builder
        this.config = config;
        this.userProvider = userProvider;
        this.userLookupProvider = userLookupProvider;
        this.offline = offline;
        featureControlCache = new SimpleMemoryFeatureCache();

        if(offline){
            restClient = new RestClientMock(apiKey, config);
            eventHandler = new EventsClientMock(config, restClient);
        }else{
            restClient = new RestClientImpl(apiKey, config);
            eventHandler = new EventsClientImpl(config, restClient);
        }


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

    public Evaluate evaluate (String featureKey, String userId) {
        FeatureflowUser user = userLookupProvider==null?new FeatureflowUser(userId):userLookupProvider.getUser(userId);
        Evaluate e = new Evaluate(this, featureKey, user);
        return e;
    }

    public Evaluate evaluate(String featureKey) {
        //create an anonymous user
        FeatureflowUser user = new FeatureflowUser();
        return evaluate(featureKey, user);
    }

    public Map<String, String> evaluateAll(FeatureflowUser user){
        Map<String, String> result = new HashMap<>();
        for(String s: featureControlCache.getAll().keySet()){
            result.put(s, eval(s, user));
        }
        return result;
    }

    private String eval(String featureKey, FeatureflowUser user) {

        String failoverVariant = (featuresMap.get(featureKey)!=null&&featuresMap.get(featureKey).failoverVariant!=null)?featuresMap.get(featureKey).failoverVariant: Variant.off;
        FeatureControl control = featureControlCache.get(featureKey);
        if(!offline&&!featureControlStreamClient.initialized()){
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
        user.getSessionAttributes().put(FeatureflowUser.FEATUREFLOW_DATE, new JsonPrimitive(FeatureflowUser.toIso(new DateTime())));
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
        private FeatureflowUserLookupProvider userLookupProvider;
        private List<Feature> features = new ArrayList<>();
        private boolean offline = false; //put there rather than configuration for convenience

        public Builder (String apiKey){
            this.apiKey = apiKey;
        }

        public Builder withOffline(boolean offline){
            this.offline = offline;
            return this;
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
        public Builder withUserLookupProvider(FeatureflowUserLookupProvider userLookupProvider){
            this.userLookupProvider = userLookupProvider;
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
            return new FeatureflowClient(apiKey, features, config, featureControlCallbackHandlers, userProvider, userLookupProvider, offline);
        }
    }

    public class Evaluate {
        private final String evaluateResult;
        private final String featureKey;
        private final FeatureflowUser user;
        private final FeatureflowClient client;

        Evaluate(FeatureflowClient featureflowClient, String featureKey, FeatureflowUser user) {
            this.featureKey = featureKey;
            this.user = user;
            this.evaluateResult = featureflowClient.eval(featureKey, user);
            this.client = featureflowClient;
        }

        Evaluate(FeatureflowClient featureflowClient, String featureKey, String userId) {
            this.featureKey = featureKey;
            this.user =
                    userProvider!=null? userProvider.getUser():
                    userLookupProvider!=null&&userId!=null?userLookupProvider.getUser(userId):
                    new FeatureflowUser(userId);
            this.client = featureflowClient;
            this.evaluateResult = featureflowClient.eval(featureKey, user);
        }
        public boolean isOn(){
            return is(Variant.on);
        }
        public boolean isOff(){
            return is(Variant.off);
        }
        public boolean is(String variant){
            if(!client.offline)eventHandler.queueEvent(new Event(featureKey, Event.EVALUATE_EVENT, user, evaluateResult, variant));
            return variant.equals(evaluateResult);
        }
        public String value(){
            if(!client.offline)eventHandler.queueEvent(new Event(featureKey, Event.EVALUATE_EVENT, user, evaluateResult, null));
            return evaluateResult;
        }
    }
}
