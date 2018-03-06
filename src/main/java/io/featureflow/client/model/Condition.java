package io.featureflow.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.featureflow.client.FeatureflowUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oliver on 18/11/16.
 */
public class Condition {
    public String target;    //name, age, date
    public Operator operator; // = < > like in out
    public List<JsonPrimitive> values = new ArrayList<>(); //some value 1,2,dave,timestamp,2016-01-11-10:10:10:0000UTC

    public Condition() {
    }

    public Condition(String target, Operator operator, List<JsonPrimitive> values) {
        this.target = target;
        this.operator = operator;
        this.values = values;
    }

    public boolean matches(FeatureflowUser user) {
        //see if context contains target
        if (user == null || (user.getAttributes() == null && user.getSessionAttributes() == null)) return false;
        Map<String, JsonElement> combined = new HashMap<>();
        combined.putAll(user.getAttributes());
        combined.putAll(user.getSessionAttributes());
        for(String attributeKey : combined.keySet()){
            if(attributeKey.equals(target)){
                //compare the value using the comparator
                JsonElement contextValue = combined.get(attributeKey);
                if(contextValue==null) return false; //does not match if there is no matching value
                if(contextValue.isJsonArray()){ //if the context value is an array of values
                    JsonArray ar = contextValue.getAsJsonArray();
                    for (JsonElement jsonElement : ar) {//return true if any of the list of context values for the key matches
                        if (operator.evaluate(jsonElement.getAsJsonPrimitive(), values))return true;
                    }
                    return false; //else return false
                }
                return operator.evaluate(contextValue.getAsJsonPrimitive(), values); //if its a single value then just return the eval
            }
            return operator.evaluate(combined.get(target).getAsJsonPrimitive(), values); //if its a single value then just return the eval
        }
        return false;

    }
}
