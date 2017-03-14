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
                .withBaseUri(TestConfiguration.LOCAL_BASE_URL)
                .withStreamBaseUri(TestConfiguration.LOCAL_BASE_STREAM_URL)
                .withWaitForStartup(5000l)
                .build();

        FeatureFlowContext context = FeatureFlowContext.keyedContext("uniqueuserkey1")
                .withValue("tier", "silver")
                .withValue("age", 32)
                .withValue("signup_date", new DateTime(2017, 1, 1, 12, 0, 0, 0))
                .withValue("user_role", "pvt_tester")
                .withValue("name", "Oliver Oldfield-Hodge")
                .build();


        FeatureFlowClient client = new FeatureFlowClient.Builder("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1OGM3Nzk0OTczOTdiM2Y1OTlhODI5ZTciLCJhdXRoIjoiUk9MRV9FTlZJUk9OTUVOVCJ9.JvxNDkwdWTsMRXHWzt7K6GkpTGsf2LuLM-sRlcgt4fIZIemSoxahniAuhWPVkBesW0SaodCsXdrdMHrRTBqDsg")
                .withFeature(new Feature("f1", Variant.off))
                .withFeatures(
                        Arrays.asList(
                            new Feature("example-server-feature-3", Variant.off),
                            new Feature("example-feature-2", Variant.off)
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