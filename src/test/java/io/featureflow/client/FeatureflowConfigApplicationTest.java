package io.featureflow.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The application tag is write-only telemetry keyed by a strict slug — an invalid value
 * must be dropped (no header sent), never mangled or thrown on.
 */
public class FeatureflowConfigApplicationTest {

    @Test
    public void validSlugPassesThrough() {
        assertEquals("checkout-api", FeatureflowConfig.sanitiseApplication("checkout-api"));
        assertEquals("checkout_api.v2", FeatureflowConfig.sanitiseApplication("checkout_api.v2"));
    }

    @Test
    public void caseAndWhitespaceAreForgiven() {
        assertEquals("featureflow-server", FeatureflowConfig.sanitiseApplication("  Featureflow-Server "));
    }

    @Test
    public void invalidValuesAreDropped() {
        assertNull(FeatureflowConfig.sanitiseApplication("checkout api!"));
        assertNull(FeatureflowConfig.sanitiseApplication("a".repeat(65)));
        assertNull(FeatureflowConfig.sanitiseApplication(""));
        assertNull(FeatureflowConfig.sanitiseApplication(null));
    }

    @Test
    public void builderCarriesTheSanitisedTagIntoTheConfig() {
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withApplication("Featureflow-Server")
                .build();
        assertEquals("featureflow-server", config.getApplication());
    }

    @Test
    public void invalidBuilderValueMeansNoTag() {
        FeatureflowConfig config = FeatureflowConfig.builder()
                .withApplication("not a slug!")
                .build();
        assertNull(config.getApplication());
    }
}
