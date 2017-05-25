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

import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
public class FeatureflowClientImpl implements FeatureflowClient {


    private static final Logger logger = LoggerFactory.getLogger(FeatureflowClientImpl.class);
    private final FeatureflowConfig config;
    private final FeatureControlStreamClient featureControlStreamClient; // manages pubsub events to update a feature control
    private final FeatureControlCache featureControlCache; //holds the featureControls
    private final RestClient restClient; //manages retrieving features and pushing updates
    private final FeatureEventHandler eventHandler;
    private final Map<String, Feature> featuresMap = new HashMap<>(); //this contains code registered features and failovers
    private Queue<FeatureControlCallbackHandler> handlers;

    FeatureflowClientImpl(String apiKey, List<Feature> features, FeatureflowConfig config, Map<CallbackEvent, List<FeatureControlCallbackHandler>> callbacks) {
        //set config, use a builder
        this.config = config;

        featureControlCache = new SimpleMemoryFeatureCache();
        restClient = new RestClient(apiKey, config);
        //featureControlEventHandler = new FeatureControlEventHandler(restClient);
        eventHandler = new FeatureEventHandler(config, restClient);

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

    @Override
    public Evaluate evaluate(String featureKey, FeatureflowContext featureflowContext) {
        Evaluate e = new Evaluate(this, featureKey, featureflowContext);
        return e;

    }

    @Override
    public Evaluate evaluate(String featureKey) {
        //create and empty context
        FeatureflowContext featureflowContext = FeatureflowContext.context().build();
        return evaluate(featureKey, featureflowContext);
    }

    @Override
    public Map<String, String> evaluateAll(FeatureflowContext featureflowContext){
        Map<String, String> result = new HashMap<>();
        for(String s: featureControlCache.getAll().keySet()){
            result.put(s, eval(s, featureflowContext));
        }
        return result;
    }

    protected String eval(String featureKey, FeatureflowContext featureflowContext) {
        String failoverVariant = (featuresMap.get(featureKey)!=null&&featuresMap.get(featureKey).failoverVariant!=null)?featuresMap.get(featureKey).failoverVariant: Variant.off;
        FeatureControl control;
        if(!featureControlStreamClient.initialized()){
            logger.warn("FeatureFlow is not initialized yet.");
            control = featureControlCache.get(featureKey);
            if(control == null){
                return failoverVariant;
            }
        }
        control = featureControlCache.get(featureKey);

        //add featureflow.context
        addAdditionalContext(featureflowContext);

        if(control==null){
            logger.error("Unknown Feature {}, returning failoverVariant value of {}", featureKey, failoverVariant);
            //featureControlEventHandler.saveEvent(featureKey, failoverVariant, featureflowContext);
            eventHandler.queueEvent(new Event(featureKey, Event.EVALUATE_EVENT, featureflowContext, failoverVariant));
            return failoverVariant;
        }

        String variant = control.evaluate(featureflowContext);

        //featureControlEventHandler.saveEvent(featureKey, variant, featureflowContext);
        eventHandler.queueEvent(new Event(featureKey, Event.EVALUATE_EVENT, featureflowContext, variant));
        return variant;

    }

    private void addAdditionalContext(FeatureflowContext featureflowContext) {
        /*{
            "key" : "featureflow.hourofday",
                "name" : "Hour of Day",
                "type" : "number",
                "owner" : "system"
        }*/
        featureflowContext.values.put(FeatureflowContext.FEATUREFLOW_HOUROFDAY, new JsonPrimitive(LocalTime.now().getHour()));
        featureflowContext.values.put(FeatureflowContext.FEATUREFLOW_DATE, new JsonPrimitive(FeatureflowContext.Builder.toIso(new DateTime())));
    }
    public void close() throws IOException {
    /*    this.eventProcessor.close();
        if (this.updateProcessor != null) {
            this.updateProcessor.close();
        }*/
    }
}
