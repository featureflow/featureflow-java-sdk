package io.featureflow.client;

import com.google.gson.JsonPrimitive;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.regex.Pattern;

/**
 * Created by oliver on 26/05/2016.
 */
public enum Operator {
    equals {
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            if(contextValue.equals(targetValue))return true;
            if (contextValue.isString() && targetValue.isString()
                    && contextValue.getAsString().equals(targetValue.getAsString()))
                return true;

            if (contextValue.isNumber() && targetValue.isNumber()) {
                return contextValue.getAsDouble() == targetValue.getAsDouble();
            }
            return false;
        }
    },
    testRuleEquals{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            return contextValue.isNumber() && targetValue.isNumber() && contextValue.getAsDouble() > targetValue.getAsDouble();
        }
    },
    lessThan{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            return contextValue.isNumber() && targetValue.isNumber() && contextValue.getAsDouble() < targetValue.getAsDouble();
        }
    },
    greaterThan{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            return contextValue.isNumber() && targetValue.isNumber() && contextValue.getAsDouble() > targetValue.getAsDouble();
        }
    },
    greaterThanOrEqual{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            return contextValue.isNumber() && targetValue.isNumber() && contextValue.getAsDouble() >= targetValue.getAsDouble();
        }
    },
    lessThanOrEqual{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            return contextValue.isNumber() && targetValue.isNumber() && contextValue.getAsDouble() <= targetValue.getAsDouble();
        }
    },
    startsWith{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            return contextValue.isString() && targetValue.isString()
                    && contextValue.getAsString().startsWith(targetValue.getAsString());
        }
    },
    endsWith{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            return contextValue.isString() && targetValue.isString()
                    && contextValue.getAsString().endsWith(targetValue.getAsString());
        }
    },
    matches{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            return targetValue.isString() && contextValue.isString()
                    && Pattern.matches(targetValue.getAsString(),contextValue.getAsString());
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
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            return contextValue.isString() && targetValue.isString() && contextValue.getAsString().contains(targetValue.getAsString());
        }
    }, //fuzzy match
    before{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            DateTime contextDateTime = getDateTime(contextValue);
            if (contextDateTime != null) {
                DateTime cDateTime = getDateTime(targetValue);
                if (cDateTime != null) {
                    return contextDateTime.isBefore(cDateTime);
                }
            }
            return false;
        }
    }, //date before
    after{
        @Override
        public boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue){
            DateTime contextDateTime = getDateTime(contextValue);
            if (contextDateTime != null) {
                DateTime cDateTime = getDateTime(targetValue);
                if (cDateTime != null) {
                    return contextDateTime.isAfter(cDateTime);
                }
            }
            return false;
        }
    }; //date after

    abstract boolean evaluate(JsonPrimitive contextValue, JsonPrimitive targetValue);

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
