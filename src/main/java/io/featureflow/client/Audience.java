package io.featureflow.client;

import java.util.List;

/**
 * Created by oliver on 18/11/16.
 */
public class Audience {
    String id;   //the audience has an id if it has been saved
    String name;
    List<Condition> conditions;

    public boolean matches(FeatureFlowContext context) {
        if(conditions==null||conditions.size()==0)return true;
        for (Condition condition : conditions) {
            if(condition.matches(context)){
                return true;
            }
        }
        return false;
    }
}
