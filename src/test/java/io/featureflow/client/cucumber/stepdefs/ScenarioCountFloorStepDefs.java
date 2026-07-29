package io.featureflow.client.cucumber.stepdefs;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A floor on how many Cucumber scenarios the suite is allowed to run.
 *
 * Cucumber-JVM already fails the build on an undefined step, and the JUnit Platform suite already
 * fails when it discovers no tests at all. Neither catches a *partial* collapse: drop one
 * &lt;include&gt; from the testbed &lt;testResource&gt; block in pom.xml, or let the testbed submodule
 * fall behind, and the remaining scenarios still pass while a whole feature file quietly stops
 * being exercised. That is the shape of the failure that hid 69 skipped scenarios in the Go SDK.
 *
 * This is a floor, not an exact count. Scenarios are added upstream regularly, so the number here
 * is expected to lag the real total. Raise it when the testbed grows; never lower it to make a
 * sudden drop go green without first finding out where the missing scenarios went.
 */
public class ScenarioCountFloorStepDefs {

    /**
     * 138 scenarios run today across the seven included testbed feature files.
     *
     * This floor sat at 55 while the real total was 61, and stayed green when a testbed bump
     * added fourteen scenarios that this SDK never ran — conditions.feature was missing from
     * pom.xml's include list. A floor cannot detect a file that was never included, only one
     * that stopped loading, so keep it close enough to the real total to be informative.
     */
    private static final int MIN_SCENARIOS = 130;

    private static final AtomicInteger SCENARIOS_RUN = new AtomicInteger();

    @After
    public void countScenario() {
        SCENARIOS_RUN.incrementAndGet();
    }

    @AfterAll
    public static void assertScenarioFloor() {
        int run = SCENARIOS_RUN.get();
        if (run < MIN_SCENARIOS) {
            throw new AssertionError("Only " + run + " Cucumber scenarios ran, expected at least "
                    + MIN_SCENARIOS + ". The testbed submodule is probably uninitialised or stale, or an "
                    + "<include> was dropped from the testbed <testResource> block in pom.xml.");
        }
    }
}
