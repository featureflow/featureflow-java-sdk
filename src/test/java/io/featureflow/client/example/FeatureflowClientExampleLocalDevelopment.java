package io.featureflow.client.example;

import io.featureflow.client.FeatureflowClient;
import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.FeatureflowUser;
import io.featureflow.client.model.Feature;
import io.featureflow.client.model.Variant;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This is a simple one-class example demonstrating the main features and usage of featureflow java client.
 */
public class FeatureflowClientExampleLocalDevelopment {

    FeatureflowClient featureflowClient;
    private CountDownLatch lock = new CountDownLatch(100);

    // Reads the key/base URL from the environment rather than a literal in source, so
    // no real SDK key is checked into source. Defaults to a local dev stack so this
    // still does something sensible once un-@Ignore'd without FEATUREFLOW_TEST_* set.
    private static final String API_KEY = System.getenv("FEATUREFLOW_TEST_API_KEY");
    private static final String BASE_URL = System.getenv("FEATUREFLOW_TEST_BASE_URL") != null
            ? System.getenv("FEATUREFLOW_TEST_BASE_URL")
            : "http://localhost:8080";

    @Test
    @Ignore
    public void testEvaluate() throws Exception {


        //Any Additional Config
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withStreamUri(BASE_URL + "/api/sdk/v1/features")
                .withRegisterFeatureUri(BASE_URL + "/api/sdk/v1/register")
                .withFeatureEventUri(BASE_URL + "/api/sdk/v1/events")
                .withWaitForStartup(5000l)
                .build();

        //Context - You would tend to set this at a point in time - e.g. ewhen a REST call comes in or process is called
        FeatureflowUser user = new FeatureflowUser("userId1")
                .withAttribute("tier", "silver")
                .withAttribute("age", 32)
                .withAttribute("signup_date", new DateTime(2017, 1, 1, 12, 0, 0, 0))
                .withAttribute("user_role", "standard_user")
                .withAttribute("name", "John Smith")
                .withAttribute("email", "contact@featureflow.io")
                .withSessionAttribute("sessionStartTime", new DateTime());


        //Initialise the client - You would do this as a Singleton when your servers start up.
        featureflowClient = new FeatureflowClient.Builder(API_KEY)
                .withFeatures(Arrays.asList(
                        //Actively registering features helps keep track of them across environments, reduce clutter and manage technical debt
                        new Feature(FeatureKeys.FEATURE_ONE, "red"),
                        new Feature(FeatureKeys.FEATURE_TWO, Variant.off),
                        new Feature(FeatureKeys.FEATURE_THREE, Variant.on),
                        new Feature(FeatureKeys.FEATURE_THREE, "red")
                ))
                //An optional callback can be registered when a control is updated (in this example we'll show the evaluated change using the context above)
                .withUpdateCallback(control -> {
                    System.out.println("Feature updated: " + control.getKey() + " - variant: " + control.evaluate(user) + "\n");
                    lock.countDown();
                })
                .withConfig(config).build();

        //Example evaluation calls
        System.out.println("feature-one value: " + featureflowClient.evaluate(FeatureKeys.FEATURE_ONE, user).value());
        System.out.println("feature-two is on?: " + featureflowClient.evaluate(FeatureKeys.FEATURE_TWO, user).isOn());
        System.out.println("feature-three is off?: " + featureflowClient.evaluate(FeatureKeys.FEATURE_THREE, user).isOff());
        System.out.println("feature-four is red?: " + featureflowClient.evaluate(FeatureKeys.FEATURE_FOUR, user).is("red"));

        lock.await(500000, TimeUnit.MILLISECONDS);
    }


    @Test
    public void testEvaluateWithProviderOffline() throws Exception {


        //Any Additional Config
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withWaitForStartup(5000l)
                .build();

        //Context - You would tend to set this at a point in time - e.g. ewhen a REST call comes in or process is called
        FeatureflowUser user = new FeatureflowUser("userId1")
                .withAttribute("tier", "silver")
                .withAttribute("age", 32)
                .withAttribute("signup_date", new DateTime(2017, 1, 1, 12, 0, 0, 0))
                .withAttribute("user_role", "standard_user")
                .withAttribute("name", "John Smith")
                .withAttribute("email", "oliver@featureflow.io")
                .withSessionAttribute("sessionStartTime", new DateTime());


        //Initialise the client - You would do this as a Singleton when your servers start up.
        featureflowClient = new FeatureflowClient.Builder("")
                .withOffline(true)
                .withUserProvider(() -> user)
                .withFeatures(Arrays.asList(
                        //Actively registering features helps keep track of them across environments, reduce clutter and manage technical debt
                        new Feature("login-button", "red"),
                        new Feature("logout-button", Variant.off),
                        new Feature("my-new-feature", Variant.off)
                ))
                //An optional callback can be registered when a control is updated (in this example we'll show the evaluated change using the context above)
                .withUpdateCallback(control -> {
                    System.out.println("Feature updated: " + control.getKey() + " - variant: " + control.evaluate(user) + "\n");
                    lock.countDown();
                })
                .withConfig(config).build();

        //Example evaluation calls
        System.out.println("example-feature value: " + featureflowClient.evaluate("example-feature").value());
        System.out.println("example-feature is on?: " + featureflowClient.evaluate("example-feature").isOn());
        System.out.println("example-feature is off?: " + featureflowClient.evaluate("example-feature").isOff());
        System.out.println("example-feature is red?: " + featureflowClient.evaluate("example-feature").is("red"));

        lock.await(500000, TimeUnit.MILLISECONDS);
    }
}
