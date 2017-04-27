package io.featureflow.client;

import com.google.gson.JsonPrimitive;
import io.featureflow.client.model.Event;
import io.featureflow.client.core.RestClient;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by oliver.oldfieldhodge on 25/2/17.
 */
public class RestClientTest {
    @Test
    public void postFeatureEvalEvent() throws Exception {
        String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4MzFiZThhZmQxODgzZDg4ZTQzMWEiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.1EvoDmtqOaAfYTtB3B1q7kSMp_Y27kQAa8GKM3fdHZcr1s6BQXHPW88U1j1K3Gwd4f0pHfZnSEJyZL0bd8kriA";
        FeatureflowConfig config =
                new FeatureflowConfig(false, null, null,
                        1,0,0,"http://localhost:8081", "http://localhost:7999", 10000l);


        FeatureflowContext context = new FeatureflowContext("mykey");
        context.values.put("user_role", new JsonPrimitive("admin"));
        RestClient client = new RestClient(apiKey, config);
        Event   event = new Event("feature-key1", "evaluate", context, "pink");
        client.postEvents(Arrays.asList(event));
    }

}