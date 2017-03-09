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

        //String baseUri = "http://rtm.featureflow.io";


        //ff.io local test eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1OGJmMzBkZDMwOWU1MTRmZmNkOWVjZjUiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.mBPt_HMi9J6n1gvhqyvpBF4WFOjKJEt64bC7fMeT5tk6Ki_mgiDScQ_I2Mh8zAESjFdE02XGdWLvT7CErhRjaA

        //ff.io (old) local: eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODk4Mjk2ZjNjNDUwZTAwMGFiODExNDIiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.x5_E2vGz17PjuTH20bV5VD4iuJqHFU1RFgaZl8ZX8xxktN9YOiCZDP4_jU5WeQDywTuw0fZWEygM-SejCQCh2A
        //String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODJhNzljNTFmOTBjYjAwMDcwOTA5NjQiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.mpWsZOsfhMaqTkpd-CJsSpa0RWbV6Xt090J4VQitC2_zRMDveFQW7ZVO6xbCbg0RwMpzjb6ANaCMvCmKzHXi3A";

        FeatureFlowConfig config = new FeatureFlowConfig.Builder()
                .withBaseUri(TestConfiguration.LOCAL_BASE_STREAM_URL).build();

        featureFlowClient = FeatureFlowClient.builder(TestConfiguration.API_KEY_LOCAL_TEST)
                .withConfig(config)
                .withFeatures(Arrays.asList(
                        new Feature("manage"),
                        new Feature("task")
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
                .withValue("user_role", "pvt_tester")
                .withValue("tier", "gold")
                .withValue("name", "Alisha Oldfield-Hodge")
                .build();


        FeatureFlowClient client = new FeatureFlowClient.Builder(TestConfiguration.API_KEY_LOCAL_TEST)
                .withFeatures(
                            Arrays.asList(
                                    new Feature("feature-one", "failover-variant"),
                                    new Feature("feature-one", "failover-variant")
                            ))

                //.withFeatures(Feature())
                /*.withCallback(control -> {
                    System.out.println("Received a control update event: " + control.getKey() + " variant: " + control.evaluate(context));
                    lock.countDown();
                })*/
                .withConfig(config).build();


        String evaluatedVariant = client.evaluate("example-feature", context, "failover-red").value();
        System.out.println(evaluatedVariant);
        lock.await(500000, TimeUnit.MILLISECONDS);

        System.out.println(client.evaluate("alpha", context, Variant.on));
    }
}