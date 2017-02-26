package io.featureflow.client;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Created by oliver on 26/05/2016.
 */
public class FeatureFlowClientImplTestInt {

    FeatureFlowClient featureFlowClient;

    class FctestUpdateHandler implements FeatureControlUpdateHandler {
        private final CountDownLatch latch;

        FctestUpdateHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onUpdate(FeatureControl control) {

            System.out.println(control.getKey() + " set to " + control.toString());
            this.latch.countDown();
        }
    }


    @Test
    public void isEnabled() throws Exception {
        final CountDownLatch latch = new CountDownLatch(10); //set the coundown latch to however many calls you want to test with
        //String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4MzFiZThhZmQxODgzZDg4ZTQzMWEiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.1EvoDmtqOaAfYTtB3B1q7kSMp_Y27kQAa8GKM3fdHZcr1s6BQXHPW88U1j1K3Gwd4f0pHfZnSEJyZL0bd8kriA";
        //String baseUri = "http://featureflow.dev";

        String baseUri = "http://localhost:7999";

        //String baseUri = "http://rtm.featureflow.io";
        //ff.io local: eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4Mjk2ZjNjNDUwZTAwMGFiODExNDIiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.x5_E2vGz17PjuTH20bV5VD4iuJqHFU1RFgaZl8ZX8xxktN9YOiCZDP4_jU5WeQDywTuw0fZWEygM-SejCQCh2A
        String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4Mjk2ZjNjNDUwZTAwMGFiODExNDIiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.x5_E2vGz17PjuTH20bV5VD4iuJqHFU1RFgaZl8ZX8xxktN9YOiCZDP4_jU5WeQDywTuw0fZWEygM-SejCQCh2A";
        //String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODJhNzljNTFmOTBjYjAwMDcwOTA5NjQiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.mpWsZOsfhMaqTkpd-CJsSpa0RWbV6Xt090J4VQitC2_zRMDveFQW7ZVO6xbCbg0RwMpzjb6ANaCMvCmKzHXi3A";
        FeatureFlowConfig config = new FeatureFlowConfig.Builder()
                .withBaseUri(baseUri).build();

        featureFlowClient = FeatureFlowClient.builder(apiKey)
                .withConfig(config)
                .withFeatureRegistrations(
                        Arrays.asList(
                                new FeatureRegistration("manage"),
                                new FeatureRegistration("task")
                        ))
                .withCallback(new FctestUpdateHandler(latch))
                .build();

        //System.out.println(featureFlowClient.getAllFeatureStatuses(new FeatureFlowContext("dave")));

        assertTrue(latch.await(300, TimeUnit.SECONDS)); //this is a 3 minute timeout if no response is heard



        /*
        FeatureFlowContext context = new FeatureFlowContext("dave");
        System.out.println(featureFlowClient.isEnabled("standard-login", context, false));

        try {
            Thread.sleep(3000);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        System.out.println(featureFlowClient.isEnabled("standard-login", context, false));
*/

    }

    @Test
    public void testEvaluate() throws Exception {
        FeatureFlowConfig config = FeatureFlowConfig.builder()
                .withBaseUri("http://featureflow.dev")
                .withStreamBaseUri("http://localhost:7999").build();
        //FeatureFlowContext context = FeatureFlowContext.builder().withValue("tier", "gold").build();

        FeatureFlowClient client = new FeatureFlowClient.Builder("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODE2YjE3M2EzNzc2NDAwMDdhODk1NWIiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.nt4j7v5x8TOqtAscFtF8mMwT9GF6jpEivG0dk-dANVT-EoKUY7g4jApgRQL-J_WcF2Rz3BmeqSYj2QUm-p4DRA")
                .withConfig(config).build();


        String evaluatedVariant = client.evaluate("social-login", Variant.off);
        System.out.println(evaluatedVariant);

        System.out.println(client.evaluate("social-login", Variant.on));
    }




}