package io.featureflow.client;

import io.featureflow.client.model.Feature;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Created by oliver on 26/05/2016.
 */
public class FeatureflowClientImplTestInt {

    FeatureflowClient featureflowClient;
    private CountDownLatch lock = new CountDownLatch(100);

    private  FeatureflowUser user = new FeatureflowUser("user1")
        .withAttribute("tier", "silver")
        .withAttribute("age", 32)
        .withAttribute("signup_date", new DateTime(2017, 1, 1, 12, 0, 0, 0))
        .withAttribute("name", "Oliver Oldfield-Hodge")
        .withAttribute("email", "oliver@featureflow.io")
        .withStringAttributes("user_role", Arrays.asList("pvt_tester", "administrator"));

    @Test
    public void testEvaluateWithUserProvider() throws Exception {

        String apiKey = "srv-env-d052272ca9e749b18c384837e34518b2";

        FeatureflowConfig config = FeatureflowConfig.builder()
                .withBaseUri(TestConfiguration.LOCAL_BASE_URL)
                .withStreamBaseUri(TestConfiguration.LOCAL_BASE_STREAM_URL)
                .withWaitForStartup(5000l)
                .build();

        FeatureflowUserProvider userProvider = () -> user;
               
        featureflowClient = FeatureflowClient.builder(apiKey)
                //.withConfig(config)
                .withUserProvider(userProvider)
                .withFeatures(Arrays.asList(
                        new Feature("example-feature"),
                        new Feature("facebook-login"),
                        new Feature("standard-login"),
                        new Feature("summary-dashboard"),
                        new Feature("unknown-feature", "green")

                ))
                .withUpdateCallback(control -> System.out.println("Received a control update event: " + control.getKey()))
                .withUpdateCallback(control -> {
                    System.out.println("Feature updated: " + control.getKey() + " - variant: " + control.evaluate(userProvider.getUser()) + "\n");
                    lock.countDown();
                }).build();
        String evaluatedVariant = featureflowClient.evaluate("example-feature").value();
        String unknown = featureflowClient.evaluate("unknown-feature").value();
        String nonexistant = featureflowClient.evaluate("nonexistent-feature").value();
        System.out.println(featureflowClient.evaluate(FeatureKeys.billing.name()).value());
        System.out.println(evaluatedVariant);
        lock.await(500000, TimeUnit.MILLISECONDS);

        System.out.println(featureflowClient.evaluate("alpha"));
    }

    @Test
    public void testEvaluate() throws Exception {

        String apiKey = "srv-env-";

        
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withBaseUri(TestConfiguration.LOCAL_BASE_URL)
                .withStreamBaseUri(TestConfiguration.LOCAL_BASE_STREAM_URL)
                .withWaitForStartup(5000l)
                .build();

        featureflowClient = FeatureflowClient.builder(apiKey)
                .withConfig(config)
                .withFeatures(Arrays.asList(
                        new Feature("example-feature"),
                        new Feature("facebook-login"),
                        new Feature("standard-login"),
                        new Feature("summary-dashboard")

                ))
                .withUpdateCallback(control -> System.out.println("Received a control update event: " + control.getKey()))
                .withUpdateCallback(control -> {
                    System.out.println("Feature updated: " + control.getKey() + " - variant: " + control.evaluate(user) + "\n");
                    lock.countDown();
                }).build();
        String evaluatedVariant = featureflowClient.evaluate("example-feature", user).value();
        System.out.println(featureflowClient.evaluate(FeatureKeys.billing.name()).value());
        System.out.println(evaluatedVariant);
        lock.await(500000, TimeUnit.MILLISECONDS);

        System.out.println(featureflowClient.evaluate("alpha", user));
    }
}