package io.featureflow.client;

import io.featureflow.client.core.EventsClient;
import io.featureflow.client.core.FeatureflowPollingClient;
import io.featureflow.client.model.FeatureControl;
import io.featureflow.client.model.Rule;

import java.util.Map;

/**
 * Created by oliver.oldfieldhodge on 14/3/17.
 */
public class TestAccessor {
    //test accessor to aid package scope testing without reflection
    public static boolean matches(Rule rule, FeatureflowUser user){
        return rule.matches(user);
    }

    public static void setFeatureControls(FeatureflowClient client, Map<String, FeatureControl> controls) {
        client.getFeatureControlCache().init(controls);
    }

    public static EventsClient getEventHandler(FeatureflowClient client) {
        return client.getEventHandler();
    }

    public static FeatureflowPollingClient getPollingClient(FeatureflowClient client) {
        return client.getFeatureControlPollingClient();
    }
}
