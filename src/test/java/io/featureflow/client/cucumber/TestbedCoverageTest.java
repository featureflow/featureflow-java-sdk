package io.featureflow.client.cucumber;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Fails when a feature file in the shared testbed is neither run here nor recorded as
 * deliberately skipped.
 *
 * The suite already had two safety nets and both missed the same gap. Cucumber's strict mode
 * catches a step that stopped matching; the scenario-count floor catches a suite that stopped
 * loading its files. Neither can see a file that was never included in the first place, which
 * is how conditions.feature — including the date-only and invalid-regex scenarios that exist
 * precisely to pin cross-SDK behaviour — went unrun here while the build stayed green and the
 * scenario count stayed flat across a testbed bump.
 *
 * The include list in pom.xml is the single source of truth; this test reads it rather than
 * restating it, so the two cannot drift. Adding a file upstream now breaks this test until
 * someone either wires it up or records here why it does not apply to this SDK.
 */
class TestbedCoverageTest {

    private static final Path POM = Paths.get("pom.xml");
    private static final Path GHERKIN = Paths.get("testbed", "gherkin");

    /**
     * Feature files this SDK deliberately does not run, each with the reason. An entry here is
     * a decision, not an oversight — which is the whole point of requiring one.
     */
    private static final Map<String, String> DELIBERATELY_SKIPPED = Map.of(
            "json_value.feature",
            "tagged @json-value in the testbed as JS-family-only; equivalent coverage in JsonValueTest",
            "feature_evaluation.feature",
            "no step definitions yet (SDK-BACKLOG item 10)",
            "user_builder.feature",
            "no step definitions yet (SDK-BACKLOG item 10)");

    /** Reads the {@code <include>} entries from the testbed testResource block in pom.xml. */
    private static Set<String> includedInPom() throws IOException {
        String pom = new String(Files.readAllBytes(POM), StandardCharsets.UTF_8);

        Matcher block = Pattern
                .compile("<directory>testbed/gherkin</directory>\\s*<includes>(.*?)</includes>",
                        Pattern.DOTALL)
                .matcher(pom);
        assertTrue(block.find(),
                "Could not find the testbed/gherkin <testResource> include block in pom.xml. "
                        + "If it was restructured, update this test to match — do not delete it.");

        Set<String> included = new LinkedHashSet<>();
        Matcher entry = Pattern.compile("<include>([^<]+)</include>").matcher(block.group(1));
        while (entry.find()) {
            included.add(entry.group(1).trim());
        }
        return included;
    }

    private static Set<String> presentInTestbed() throws IOException {
        try (Stream<Path> files = Files.list(GHERKIN)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".feature"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    @Test
    void everyTestbedFeatureFileIsRunOrExplicitlySkipped() throws IOException {
        assertTrue(Files.isDirectory(GHERKIN),
                "testbed/gherkin is missing — run `git submodule update --init`.");

        Set<String> present = presentInTestbed();
        assertTrue(!present.isEmpty(),
                "testbed/gherkin contains no feature files — the submodule is probably an empty checkout.");

        Set<String> included = includedInPom();

        Set<String> unaccounted = new TreeSet<>(present);
        unaccounted.removeAll(included);
        unaccounted.removeAll(DELIBERATELY_SKIPPED.keySet());
        if (!unaccounted.isEmpty()) {
            fail("Testbed feature file(s) " + unaccounted + " are neither included in pom.xml nor "
                    + "listed as deliberately skipped in this test. Add step definitions and an "
                    + "<include> entry, or record here why this SDK does not run them.");
        }

        // The inverse: a file removed or renamed upstream leaves a stale entry behind, and the
        // include would then silently match nothing.
        Set<String> stale = new TreeSet<>(included);
        stale.addAll(DELIBERATELY_SKIPPED.keySet());
        stale.removeAll(present);
        if (!stale.isEmpty()) {
            fail("Feature file(s) " + stale + " are referenced by pom.xml or by this test but no "
                    + "longer exist in the testbed — they were probably renamed upstream.");
        }

        // A file cannot be both run and recorded as skipped; that reads as a decision reversed
        // in one place and not the other.
        List<String> contradictory = included.stream()
                .filter(DELIBERATELY_SKIPPED::containsKey)
                .collect(Collectors.toList());
        if (!contradictory.isEmpty()) {
            fail("Feature file(s) " + contradictory + " are included in pom.xml but also listed as "
                    + "deliberately skipped in this test. Remove them from DELIBERATELY_SKIPPED.");
        }
    }
}
