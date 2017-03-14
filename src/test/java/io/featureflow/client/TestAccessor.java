package io.featureflow.client;

import com.google.gson.JsonElement;

import java.util.Map;

/**
 * Created by oliver.oldfieldhodge on 14/3/17.
 */
public class TestAccessor {
    //test accessor to aid package scope testing without reflection
    public static boolean matches(Rule rule, FeatureFlowContext context){
        return rule.matches(context);
    }

    public static void setContextValues(FeatureFlowContext context, Map<String, JsonElement> contextVals) {
        context.values = contextVals;
    }
}
