package io.featureflow.client;



import com.google.gson.JsonPrimitive;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
public class FeatureFlowClientImpl implements FeatureFlowClient {


    private static final Logger logger = LoggerFactory.getLogger(FeatureFlowClientImpl.class);
    private final FeatureFlowConfig config;
    private final FeatureControlStreamClient featureControlStreamClient; // manages pubsub events to update a feature control
    private final FeatureControlCache featureControlCache; //holds the featureControls
    private final FeatureflowRestClient featureflowRestClient; //manages retrieving features and pushing updates
    private final FeatureControlEventHandler featureControlEventHandler;
    private final Map<String, Feature> featuresMap = new HashMap<>();
    private Queue<FeatureControlUpdateHandler> handlers;

    FeatureFlowClientImpl(String apiKey, List<Feature> features, FeatureFlowConfig config, FeatureControlUpdateHandler callback) {
        //set config, use a builder
        this.config = config;
        //Actively defining registrations helps alert if features are available in an environment
        if(features !=null&& features.size()>0){
            for (Feature feature : features) {
                featuresMap.put(feature.key, feature);
            }
        }
        featureControlCache = new SimpleMemoryFeatureCache();
        featureflowRestClient = new FeatureflowRestClient(apiKey, config);
        featureControlStreamClient = new FeatureControlStreamClient(apiKey, config, featureControlCache, callback);
        featureControlEventHandler = new FeatureControlEventHandler(featureflowRestClient);

        try {
            featureflowRestClient.registerFeatureControls(featuresMap);
        } catch (IOException e) {
            logger.error("Problem registering reature controls", e);
        }
        Future<Void> startFuture = featureControlStreamClient.start();
        if (config.waitForStartup > 0L) {
            logger.info("Waiting for Featureflow to inititalise");
            try {
                startFuture.get(20000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.error("Timeout waiting for Featureflow client initialise");
            } catch (Exception e) {
                logger.error("Exception waiting for Featureflow client to initialise", e);
            }
        }
    }

    @Override
    public Evaluate evaluate(String featureKey, FeatureFlowContext featureFlowContext) {
        Evaluate e = new Evaluate(this, featureKey, featureFlowContext);
        return e;

    }

    @Override
    public Evaluate evaluate(String featureKey) {
        //create and empty context
        FeatureFlowContext featureFlowContext = FeatureFlowContext.context().build();
        return evaluate(featureKey, featureFlowContext);
    }

    protected String eval(String featureKey, FeatureFlowContext featureFlowContext) {
        String failoverVariant = featuresMap.get(featureKey)!=null?featuresMap.get(featureKey).failoverVariant:Variant.off;

        if(!featureControlStreamClient.initialized()){
            logger.warn("FeatureFlow is not initialized yet, returning default value");
            return failoverVariant;
        }
        FeatureControl control = featureControlCache.get(featureKey);
        if(control==null){
            logger.error("Unknown Feature {}, returning failoverVariant value of {}", featureKey, failoverVariant);
            featureControlEventHandler.saveEvent(null, featureKey, failoverVariant, featureFlowContext);
            return failoverVariant;
        }
        //add featureflow.context
        addAdditionalContext(featureFlowContext);
        String variant = control.evaluate(featureFlowContext);

        featureControlEventHandler.saveEvent(control.featureId, featureKey, variant, featureFlowContext);
        return variant;

    }

    private void addAdditionalContext(FeatureFlowContext featureFlowContext) {
        featureFlowContext.values.put(FeatureFlowContext.FEATUREFLOW_DATE, new JsonPrimitive(FeatureFlowContext.Builder.toIso(new DateTime())));
    }
    public void close() throws IOException {
    /*    this.eventProcessor.close();
        if (this.updateProcessor != null) {
            this.updateProcessor.close();
        }*/
    }
}
