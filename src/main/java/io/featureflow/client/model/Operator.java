package io.featureflow.client.model;

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
            if(targetValues==null)return false;
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
            if(targetValues==null)return true;
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

    public abstract boolean evaluate(JsonPrimitive contextValue, List<JsonPrimitive> targetValues);

    /**
     * ISO-8601 parser anchored to UTC.
     *
     * The dashboard's date picker emits date-only condition values such as 2026-07-03. The shared
     * SDK contract (testbed CONTRACT.md, "Operators", decided 2026-07-29) requires every SDK to
     * read such a value as UTC midnight - 2026-07-03T00:00:00Z. Without withZoneUTC() this parser
     * falls back to the JVM's default timezone, so the same rule would take effect at a different
     * instant on every host and a scheduled rollout would be non-deterministic across a fleet.
     * UTC is the only reading that is identical everywhere, and it is what sdk-server and the
     * JavaScript SDK already do via Date.parse.
     *
     * A value that carries its own offset or a trailing Z already names an instant: Joda computes
     * that instant from the parsed offset and only then converts it to this formatter's zone, so
     * such values are represented in UTC but never shifted.
     */
    private static final DateTimeFormatter ISO_PARSER = ISODateTimeFormat.dateTimeParser().withZoneUTC();

    protected static DateTime getDateTime(JsonPrimitive date) {
        if (date.isNumber()) {
            // Epoch milliseconds are absolute; read them in UTC so every parsed value shares a zone.
            return new DateTime(date.getAsLong(), DateTimeZone.UTC);
        } else if (date.isString()) {
            try {
                return ISO_PARSER.parseDateTime(date.getAsString());
            } catch (IllegalArgumentException ex) {
                // Unparseable: fall through to null so the operator returns no-match rather than
                // throwing. There is no second parser to try - new DateTime(String, UTC) delegates
                // to this same ISO parser, so it could only fail identically.
            }
        }
        return null;
    }
}
