package io.featureflow.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.featureflow.client.FeatureFlowContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oliver on 18/11/16.
 */
public class Condition {
    public String target;    //name, age, date
    public Operator operator; // = < > like in out
    public List<JsonPrimitive> values = new ArrayList<>(); //some value 1,2,dave,timestamp,2016-01-11-10:10:10:0000UTC

    public Condition() {}
    public Condition(String target, Operator operator, List<JsonPrimitive> values) {
        this.target = target;
        this.operator = operator;
        this.values = values;
    }

    public boolean matches(FeatureFlowContext context) {
        //see if context contains target
        if(context == null || context.values==null)return false;
        for(String key : context.values.keySet()){
            if(key.equals(target)){
                //compare the value using the comparator
                JsonElement contextValue = context.values.get(key);
                if(contextValue.isJsonArray()){ //if the context value is an array of values
                    JsonArray ar = contextValue.getAsJsonArray();
                    for (JsonElement jsonElement : ar) {//return true if any of the list of context values for the key matches
                        if (operator.evaluate(jsonElement.getAsJsonPrimitive(), values))return true;
                    }
                    return false; //else return false
                }
                return operator.evaluate(context.values.get(key).getAsJsonPrimitive(), values); //if its a single value then just return the eval

            }
        }
        return false;
    }
}
