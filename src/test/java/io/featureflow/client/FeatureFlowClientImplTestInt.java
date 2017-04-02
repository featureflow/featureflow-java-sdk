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
    private CountDownLatch lock = new CountDownLatch(100);
    @Test
    public void testEvaluate() throws Exception {


        FeatureFlowConfig config = FeatureFlowConfig.builder()
              //  .withBaseUri(TestConfiguration.LOCAL_BASE_URL)
              //  .withStreamBaseUri(TestConfiguration.LOCAL_BASE_STREAM_URL)
                .withOffline(true)
                .withWaitForStartup(5000l)
                .build();

        FeatureFlowContext context = FeatureFlowContext.keyedContext("uniqueuserkey1")
                .withValue("tier", "silver")
                .withValue("age", 32)
                .withValue("signup_date", new DateTime(2017, 1, 1, 12, 0, 0, 0))
                .withValue("name", "Oliver Oldfield-Hodge")
                .withValue("email", "oliver@featureflow.io")
                .withValues("user_role", Arrays.asList("pvt_tester", "administrator"))
                .build();


        FeatureFlowClient client = new FeatureFlowClient.Builder("api-key")
                .withFeatures(Arrays.asList(
                    new Feature("example-server-feature-q"),
                    new Feature("example-feature-2", Variant.off),
                    new Feature("example-feature-3", Arrays.asList(
                            new Variant("red", "Red Variant"),
                            new Variant("blue", "Blue Variant")
                    ),Variant.off)
                ))
                .withCallback(control -> {
                    System.out.println("Feature updated: " + control.getKey() + " - variant: " + control.evaluate(context) + "\n");
                    lock.countDown();
                })
                .withConfig(config).build();
        String evaluatedVariant = client.evaluate("example-feature", context).value();
        System.out.println(evaluatedVariant);
        lock.await(500000, TimeUnit.MILLISECONDS);

        System.out.println(client.evaluate("alpha", context));
    }
}