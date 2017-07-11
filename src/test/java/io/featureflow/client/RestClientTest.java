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
    //@Test
    public void postFeatureEvalEvent() throws Exception {
        String apiKey = "API_KEY";
        FeatureflowConfig config =
                new FeatureflowConfig(false, null, null,
                        1,0,0,"http://localhost:8081", "http://localhost:7999", 10000l);


        FeatureflowContext context = new FeatureflowContext("mykey");
        context.values.put("user_role", new JsonPrimitive("admin"));
        RestClient client = new RestClient(apiKey, config);
        Event   event = new Event("feature-key1", "evaluate", context, "pink", "green");
        client.postEvents(Arrays.asList(event));
    }

}