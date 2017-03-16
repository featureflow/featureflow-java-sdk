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
public class FeatureFlowClientExample {

    FeatureFlowClient featureFlowClient;
    private CountDownLatch lock = new CountDownLatch(100);
    @Test
    public void testEvaluate() throws Exception {


        //Any Additional Config
        FeatureFlowConfig config = FeatureFlowConfig.builder()
                .withWaitForStartup(5000l)
                .build();

        //Context - You would tend to set this at a point in time - e.g. ewhen a REST call comes in or process is called
        FeatureFlowContext context = FeatureFlowContext.keyedContext("uniqueuserkey1")
                .withValue("tier", "silver")
                .withValue("age", 32)
                .withValue("signup_date", new DateTime(2017, 1, 1, 12, 0, 0, 0))
                .withValue("user_role", "standard_user")
                .withValue("name", "Rudi Simic")
                .withValue("email", "oliver@featureflow.io")
                .build();


        //Initialise the client - You would do this as a Singleton when your servers start up.
        featureFlowClient = new FeatureFlowClient.Builder("")
                .withFeatures(Arrays.asList(
                        //Actively registering features helps keep track of them across environments, reduce clutter and manage technical debt
                        new Feature("login-button", "red"),
                        new Feature("logout-button", Variant.off),
                        new Feature("my-new-feature", Variant.off)
                ))
                //An optional callback can be registered when a control is updated (in this example we'll show the evaluated change using the context above)
                .withCallback(control -> {
                    System.out.println("Feature updated: " + control.getKey() + " - variant: " + control.evaluate(context) + "\n");
                    lock.countDown();
                })
                .withConfig(config).build();

        //Example evaluation calls
        System.out.println("example-feature value: " + featureFlowClient.evaluate("example-feature", context).value());
        System.out.println("example-feature is on?: " + featureFlowClient.evaluate("example-feature", context).isOn());
        System.out.println("example-feature is off?: " + featureFlowClient.evaluate("example-feature", context).isOff());
        System.out.println("example-feature is red?: " + featureFlowClient.evaluate("example-feature", context).is("red"));

        lock.await(500000, TimeUnit.MILLISECONDS);
    }
}
