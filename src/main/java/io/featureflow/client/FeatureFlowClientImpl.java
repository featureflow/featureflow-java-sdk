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

import static io.featureflow.client.FeatureFlowContext.Builder.toIso;


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
    private final FeatureControlStreamManager featureControlStreamManager; // manages pubsub events to update a feature control
    private final FeatureControlRepository featureControlRepository; //holds the featureControls
    private final FeatureControlRestClient featureControlRestClient; //manages retrieving features and pushing updates
    private final FeatureControlEventHandler featureControlEventHandler;

    private final Map<String, FeatureRegistration> featureRegistrationsMap = new HashMap<>();
    private Queue<FeatureControlUpdateHandler> handlers;

    public FeatureFlowClientImpl(String apiKey){
        this(apiKey, null, new FeatureFlowConfig.Builder().build(), null);
    }
    FeatureFlowClientImpl(String apiKey, List<FeatureRegistration> featureRegistrations, FeatureFlowConfig config, FeatureControlUpdateHandler callback) {
        //set config, use a builder
        this.config = config;
        //Actively defining registrations helps alert if features are available in an environment
        if(featureRegistrations!=null&&featureRegistrations.size()>0){
            for (FeatureRegistration featureRegistration : featureRegistrations) {
                featureRegistrationsMap.put(featureRegistration.key, featureRegistration);
            }
        }
        featureControlRepository = new SimpleMemoryFeatureRepository();
        featureControlRestClient = new FeatureControlRestClient(apiKey, config);
        featureControlStreamManager = new FeatureControlStreamManager(apiKey, config, featureControlRepository, callback);
        featureControlEventHandler = new FeatureControlEventHandler(featureControlRestClient);

        Future<Void> startFuture = featureControlStreamManager.start();
        if (config.waitForStartup > 0L) {
            logger.info("Waiting for Featureflow to inititalise");
            try {
                startFuture.get(20000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.error("Timeout encountered waiting for Featureflow client initialise");
            } catch (Exception e) {
                logger.error("Exception encountered waiting for Featureflow client to initialise", e);
            }
        }
    }

    @Override
    public Evaluate evaluate(String featureKey, FeatureFlowContext featureFlowContext, String failoverVariant) {
        Evaluate e = new Evaluate(this, featureKey, featureFlowContext, failoverVariant);
        return e;

    }

    @Override
    public Evaluate evaluate(String featureKey, String defaultVariant) {
        //create and empty context
        FeatureFlowContext featureFlowContext = FeatureFlowContext.context().build();
        return evaluate(featureKey, featureFlowContext, defaultVariant);
    }

    protected String eval(String featureKey, FeatureFlowContext featureFlowContext, String failoverVariant) {
        if(!featureControlStreamManager.initialized()){
            logger.warn("FeatureFlow is not initialized yet, returning default value");
            return failoverVariant;
        }
        FeatureControl control = featureControlRepository.get(featureKey);
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
        featureFlowContext.values.put(FeatureFlowContext.FEATUREFLOW_DATE, new JsonPrimitive(toIso(new DateTime())));
    }
    public void close() throws IOException {
    /*    this.eventProcessor.close();
        if (this.updateProcessor != null) {
            this.updateProcessor.close();
        }*/
    }
}
