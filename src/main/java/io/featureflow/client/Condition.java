package io.featureflow.client;

import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oliver on 18/11/16.
 */
public class Condition {
    String target;    //name, age, date
    Operator operator; // = < > like in out
    List<JsonPrimitive> values = new ArrayList<>(); //some value 1,2,dave,timestamp,2016-01-11-10:10:10:0000UTC

    public boolean matches(FeatureFlowContext context) {
        //see if context contains target
        if(context.values==null)return false;
        for(String key : context.values.keySet()){
            if(key.equals(target)){
                //compare the value using the comparator
                return operator.evaluate(context.values.get(key).getAsJsonPrimitive(), values);
            }
        }
        return false;
    }
}
