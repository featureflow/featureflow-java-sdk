package io.featureflow.client;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Future;


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

    private final Map<String, FeatureRegistration> featureRegistrationsMap = new HashMap<>();
    private Queue<FeatureControlUpdateHandler> handlers;

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
        featureControlStreamManager = new FeatureControlStreamManager(apiKey, config, featureControlRepository, featureControlRestClient, callback);

        Future<Void> startFuture = featureControlStreamManager.start();
    }

    @Override
    public String evaluate(String featureKey, FeatureFlowContext featureFlowContext, String failoverVariant) {
        if(!featureControlStreamManager.initialized()){
            logger.warn("FeatureFlow is not initialized yet, returning default value");
            return failoverVariant;
        }

        FeatureControl control = featureControlRepository.get(featureKey);
        if(control==null){
            logger.error("Unknown Feature {}, returning failoverVariant value of {}", featureKey, failoverVariant);
            return failoverVariant;
        }



        return control.evaluate(featureFlowContext);


    }

    @Override
    public String evaluate(String featureKey, String defaultVariant) {
        //create and empty context
        FeatureFlowContext featureFlowContext = FeatureFlowContext.context().build();
        return evaluate(featureKey, featureFlowContext, defaultVariant);
    }

    public void close() throws IOException {
    /*    this.eventProcessor.close();
        if (this.updateProcessor != null) {
            this.updateProcessor.close();
        }*/
    }
}
