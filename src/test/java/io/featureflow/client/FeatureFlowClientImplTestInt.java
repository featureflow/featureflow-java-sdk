package io.featureflow.client;

import io.featureflow.client.model.Feature;
import io.featureflow.client.model.Variant;
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
                .withBaseUri(TestConfiguration.LOCAL_BASE_URL)
                .withStreamBaseUri(TestConfiguration.LOCAL_BASE_STREAM_URL)
                //.withOffline(true)
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


        FeatureFlowClient client = new FeatureFlowClient.Builder("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1OGU3Mjk2OWE5ZDhkM2I4Zjk5MGJjMDMiLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.iBxNrI1ZJZfrtBACuzGEOzbIePSRDg50zN45e6vBHPEt_N6HaLmqYcfavvAY91alkCdNvhARMnTXLdADm1bj9w")
                .withFeatures(Arrays.asList(
                    new Feature("feature-1"),
                    new Feature("example-feature-2", Variant.off),
                    new Feature("example-feature-3", Arrays.asList(
                            new Variant("red", "Red Variant"),
                            new Variant("blue", "Blue Variant")
                    ),Variant.off)
                ))
                .withUpdateCallback(control -> {
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