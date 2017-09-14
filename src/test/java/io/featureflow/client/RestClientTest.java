package io.featureflow.client;

import io.featureflow.client.core.RestClientImpl;
import io.featureflow.client.model.Event;
import io.featureflow.client.core.RestClient;

import java.util.Arrays;

/**
 * Created by oliver.oldfieldhodge on 25/2/17.
 */
public class RestClientTest {
    //@Test
    public void postFeatureEvalEvent() throws Exception {
        String apiKey = "API_KEY";
        FeatureflowConfig config =
                new FeatureflowConfig(null, null,
                        1,0,0,"http://localhost:8081", "http://localhost:7999", 10000l);


        FeatureflowUser user = new FeatureflowUser("mykey");
        user.withAttribute("user_role", "admin");
        RestClient client = new RestClientImpl(apiKey, config);
        Event   event = new Event("feature-key1", "evaluate", user, "pink", "green");
        client.postEvents(Arrays.asList(event));
    }

}