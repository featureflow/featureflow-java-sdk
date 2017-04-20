package io.featureflow.client;

import com.google.gson.JsonElement;
import io.featureflow.client.model.Rule;

import java.util.Map;

/**
 * Created by oliver.oldfieldhodge on 14/3/17.
 */
public class TestAccessor {
    //test accessor to aid package scope testing without reflection
    public static boolean matches(Rule rule, FeatureflowContext context){
        return rule.matches(context);
    }

    public static void setContextValues(FeatureflowContext context, Map<String, JsonElement> contextVals) {
        context.values = contextVals;
    }
}
