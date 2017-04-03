package io.featureflow.client;

import com.google.gson.JsonPrimitive;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by oliver.oldfieldhodge on 25/2/17.
 */
public class FeatureflowRestClientTest {
    @Test
    public void postFeatureEvalEvent() throws Exception {
        String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4MzFiZThhZmQxODgzZDg4ZTQzMWEiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.1EvoDmtqOaAfYTtB3B1q7kSMp_Y27kQAa8GKM3fdHZcr1s6BQXHPW88U1j1K3Gwd4f0pHfZnSEJyZL0bd8kriA";
        FeatureFlowConfig config =
                new FeatureFlowConfig(false, null, null,
                        1,0,0,"http://localhost:8081", "http://localhost:7999", 10000l);


        FeatureFlowContext context = new FeatureFlowContext("mykey");
        context.values.put("user_role", new JsonPrimitive("admin"));
        FeatureflowRestClient client = new FeatureflowRestClient(apiKey, config);

        FeatureEvalEvent event = new FeatureEvalEvent("feature-key1", "pink", context);
        client.postFeatureEvalEvents(Arrays.asList(event));
    }

}