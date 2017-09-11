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
public class FeatureflowClientExample {

    FeatureflowClient featureflowClient;
    private CountDownLatch lock = new CountDownLatch(100);
    @Test
    public void testEvaluate() throws Exception {


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
        System.out.println("example-feature value: " + featureflowClient.evaluate("example-feature", user).value());
        System.out.println("example-feature is on?: " + featureflowClient.evaluate("example-feature", user).isOn());
        System.out.println("example-feature is off?: " + featureflowClient.evaluate("example-feature", user).isOff());
        System.out.println("example-feature is red?: " + featureflowClient.evaluate("example-feature", user).is("red"));

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
