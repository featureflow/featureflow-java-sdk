package io.featureflow.client;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Created by oliver on 26/05/2016.
 */
public class FeatureFlowClientImplTestInt {

    FeatureFlowClient featureFlowClient;

    class FctestEventHandler implements FeatureControlEventHandler{
        private final CountDownLatch latch;

        FctestEventHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onUpdate(FeatureControl control) {

            System.out.println(control.getKey() + " set to " + control.enabled + " for environment BIGBANK LIVE");
            this.latch.countDown();
        }
    }


    @Test
    public void isEnabled() throws Exception {
        final CountDownLatch latch = new CountDownLatch(10); //set the coundown latch to however many calls you want to test with
        //private final FctestEventHandler handler = new FctestEventHandler(new CountDownLatch(1));



        //featureflow.io TEST1
        //String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1N2JiYjJiNGU0YjAyNjM5NDVhMDA4NjciLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.4WJXZ4ENJliMmJUp-7OPPKUhKErPNIKnHc5fygZlBEaLkCS_5i5eEFhd5ezW-VU6ABswspWtEysjJDgoU0vMzQ";


        //LOCALHOST BIGBANK LIVE
        //String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1N2FjMTJkM2Q0YzZiMDJjYWRkODNhYmMiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.mXnk16FkWBkr7GWAiL6n2TqHKjjN2MYOqBgXx85kdGI-NZG2t1pPU4U1XYmO8q2igm3wn98D6vtLl9CTO5iw1w";

        //featureflow.dev TEST environment key  "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODE2YjE3M2EzNzc2NDAwMDdhODk1NWIiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.nt4j7v5x8TOqtAscFtF8mMwT9GF6jpEivG0dk-dANVT-EoKUY7g4jApgRQL-J_WcF2Rz3BmeqSYj2QUm-p4DRA"
        //featureflow.dev baseUri
        //"http://localhost:8081"


        String apiKey = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODE2YjE3M2EzNzc2NDAwMDdhODk1NWIiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.nt4j7v5x8TOqtAscFtF8mMwT9GF6jpEivG0dk-dANVT-EoKUY7g4jApgRQL-J_WcF2Rz3BmeqSYj2QUm-p4DRA";
        //String baseUri = "http://featureflow.dev";
        String baseUri = "http://localhost:8081";
        FeatureFlowConfig config = new FeatureFlowConfig.Builder()
                .withBaseURI(baseUri).build();

        featureFlowClient = FeatureFlowClient.builder(apiKey)
                .withConfig(config)
                /*.withFeatureRegistrations(
                        Arrays.asList(
                                new FeatureRegistration(FeaturesDefinitions.FEATURE_ONE)
                        ))*/
                .withCallback(new FctestEventHandler(latch))
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
    public void getVariation() throws Exception {
        FeatureFlowConfig config = FeatureFlowConfig.builder().withBaseURI("http://featureflow.dev").build();
        //FeatureFlowContext context = FeatureFlowContext.builder().withValue("tier", "gold").build();
        FeatureFlowClient client = new FeatureFlowClient.Builder("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1ODE2YjE3M2EzNzc2NDAwMDdhODk1NWIiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.nt4j7v5x8TOqtAscFtF8mMwT9GF6jpEivG0dk-dANVT-EoKUY7g4jApgRQL-J_WcF2Rz3BmeqSYj2QUm-p4DRA")
                .withConfig(config).build();
        client.evaluate("social-login", Variant.off);
        System.out.println(client.evaluate("social-login", Variant.on));
    }


}