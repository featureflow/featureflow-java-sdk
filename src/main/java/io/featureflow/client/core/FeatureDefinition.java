package io.featureflow.client.core;

import java.util.List;

/**
 * Created by oliver.oldfieldhodge on 9/3/17.
 */
public interface FeatureDefinition{
    String getKey();
    String getFailover();
    List<String> getVariants();
    String name();

}
