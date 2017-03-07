package io.featureflow.client;

import com.google.gson.JsonPrimitive;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by oliver on 26/05/2016.
 */
public enum Operator {
    equals {
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            if(contextValue.equals(targetValues.get(0)))return true;
            if (contextValue.isString() && targetValues.get(0).isString()
                    && contextValue.getAsString().equals(targetValues.get(0).getAsString()))
                return true;

            if (contextValue.isNumber() && targetValues.get(0).isNumber()) {
                return contextValue.getAsDouble() == targetValues.get(0).getAsDouble();
            }
            return false;
        }
    },
    testRuleEquals{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            return contextValue.isNumber() && targetValues.get(0).isNumber() && contextValue.getAsDouble() > targetValues.get(0).getAsDouble();
        }
    },
    lessThan{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            return contextValue.isNumber() && targetValues.get(0).isNumber() && contextValue.getAsDouble() < targetValues.get(0).getAsDouble();
        }
    },
    greaterThan{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            return contextValue.isNumber() && targetValues.get(0).isNumber() && contextValue.getAsDouble() > targetValues.get(0).getAsDouble();
        }
    },
    greaterThanOrEqual{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            return contextValue.isNumber() && targetValues.get(0).isNumber() && contextValue.getAsDouble() >= targetValues.get(0).getAsDouble();
        }
    },
    lessThanOrEqual{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            return contextValue.isNumber() && targetValues.get(0).isNumber() && contextValue.getAsDouble() <= targetValues.get(0).getAsDouble();
        }
    },
    startsWith{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            return contextValue.isString() && targetValues.get(0).isString()
                    && contextValue.getAsString().startsWith(targetValues.get(0).getAsString());
        }
    },
    endsWith{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            return contextValue.isString() && targetValues.get(0).isString()
                    && contextValue.getAsString().endsWith(targetValues.get(0).getAsString());
        }
    },
    matches{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            return targetValues.get(0).isString() && contextValue.isString()
                    && Pattern.matches(targetValues.get(0).getAsString(),contextValue.getAsString());
        }
    },
    in{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            for (JsonPrimitive targetValue : targetValues) {
                if(contextValue.equals(targetValue))return true;
                if (contextValue.isString() && targetValue.isString()
                        && contextValue.getAsString().equals(targetValue.getAsString()))
                    return true;

                if (contextValue.isNumber() && targetValue.isNumber()) {
                    if(contextValue.getAsDouble() == targetValue.getAsDouble()){
                        return true;
                    }
                }

            }
            return false;
        }
    },
    notIn{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            for (JsonPrimitive targetValue : targetValues) {
                if(contextValue.equals(targetValue))return false;
                if (contextValue.isString() && targetValue.isString()
                        && contextValue.getAsString().equals(targetValue.getAsString()))
                    return false;
                if (contextValue.isNumber() && targetValue.isNumber()) {
                    if(contextValue.getAsDouble() == targetValue.getAsDouble()){
                        return false;
                    }
                }

            }
            return true;
        }
    },
    /*  IN{
          @Override
          public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
              return contextValue.equals(targetValue);
          }
      }, //must match one of a list
      NOT_IN{
          @Override
          public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
              return contextValue.equals(targetValue);
          }
      }, *///must not match any of a list
    contains{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            return contextValue.isString() && targetValues.get(0).isString() && contextValue.getAsString().contains(targetValues.get(0).getAsString());
        }
    }, //fuzzy match
    before{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            DateTime contextDateTime = getDateTime(contextValue);
            if (contextDateTime != null) {
                DateTime cDateTime = getDateTime(targetValues.get(0));
                if (cDateTime != null) {
                    return contextDateTime.isBefore(cDateTime);
                }
            }
            return false;
        }
    }, //date before
    after{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues){
            DateTime contextDateTime = getDateTime(contextValue);
            if (contextDateTime != null) {
                DateTime cDateTime = getDateTime(targetValues.get(0));
                if (cDateTime != null) {
                    return contextDateTime.isAfter(cDateTime);
                }
            }
            return false;
        }
    }; //date after

    abstract boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues);

    protected static DateTime getDateTime(JsonPrimitive date) {
        if (date.isNumber()) {
            long millis = date.getAsLong();
            return new DateTime(millis);
        } else if (date.isString()) {

                try {
                    DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
                    return parser.parseDateTime(date.getAsString());
                }catch(IllegalArgumentException ex){
                    try {
                        return new DateTime(date.getAsString(), DateTimeZone.UTC);
                    } catch (Throwable t) {}
                }
        }
        return null;
    }
}
