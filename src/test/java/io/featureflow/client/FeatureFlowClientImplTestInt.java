package io.featureflow.client;

import org.joda.time.DateTime;
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
        String baseUri = "http://localhost:7999";
        //String baseUri = "http://rtm.featureflow.io";

        String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4Mjk2ZjNjNDUwZTAwMGFiODExNDIiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.x5_E2vGz17PjuTH20bV5VD4iuJqHFU1RFgaZl8ZX8xxktN9YOiCZDP4_jU5WeQDywTuw0fZWEygM-SejCQCh2A";
        //ff.io local: eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4Mjk2ZjNjNDUwZTAwMGFiODExNDIiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.x5_E2vGz17PjuTH20bV5VD4iuJqHFU1RFgaZl8ZX8xxktN9YOiCZDP4_jU5WeQDywTuw0fZWEygM-SejCQCh2A
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
    private CountDownLatch lock = new CountDownLatch(100);
    @Test
    public void testEvaluate() throws Exception {


        FeatureFlowConfig config = FeatureFlowConfig.builder()
                .withBaseUri(TestConfiguration.LOCAL_BASE_URL)
                .withStreamBaseUri(TestConfiguration.LOCAL_BASE_STREAM_URL)
         .build();

        FeatureFlowContext context = FeatureFlowContext.keyedContext("uniqueuserkey1")
                .withValue("tier", "gold")
                .withValue("age", 32)
                .withValue("signup_date", new DateTime(2017, 1, 1, 12, 0, 0, 0))
                .withValue("user_role", "admin")
                .withValue("tier", "gold")
                .withValue("name", "Alisha Oldfield-Hodge")
                .build();

        FeatureFlowClient client = new FeatureFlowClient.Builder("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1OGI5MDIxMjQwY2JlOTljY2QwYjc3YjMiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.S1XVULDc7U1lhN0jEKUOwmUp2R5jw20FZRW6NVvsCXIr4CuP6LirlUYCU3JFzz8TSvXtLJG8MZ4nfTf5GiavLQ")
        //FeatureFlowClient client = new FeatureFlowClient.Builder("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODNjZjlkZWI0N2VlZDAwMDYwYThiMzkiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.tSbZlWyVQcJyR8ORhqiTJYrRF9DWV-fjg-x6Uq0DgN8mDIKNZdKJo33VryoyXfzeHEArMAzErcHsTSOGr_q0Gg")
                .withCallback(control -> {
                    System.out.println("Received a control update event: " + control.getKey() + " variant: " + control.evaluate(context));
                    lock.countDown();
                })
                .withConfig(config).build();


        String evaluatedVariant = client.evaluate("example-feature", context, Variant.off).value();
        System.out.println(evaluatedVariant);
        lock.await(500000, TimeUnit.MILLISECONDS);

        System.out.println(client.evaluate("alpha", context, Variant.on));
    }




}