package io.featureflow.client;

import io.featureflow.client.model.Rule;

/**
 * Created by oliver.oldfieldhodge on 14/3/17.
 */
public class TestAccessor {
    //test accessor to aid package scope testing without reflection
    public static boolean matches(Rule rule, FeatureflowUser user){
        return rule.matches(user);
    }
}
