package io.featureflow.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oliver on 23/05/2016.
 */
public class FeatureflowContext {


    public String key;
    public Map<String, JsonElement> values = new HashMap<>();


    public static final String FEATUREFLOW_KEY = "featureflow.key";
    public static final String FEATUREFLOW_DATE = "featureflow.date";
    public static final String FEATUREFLOW_HOUROFDAY = "featureflow.hourofday";

    public FeatureflowContext() {}

    public FeatureflowContext(String key) {
        this.values.put(FEATUREFLOW_KEY, new JsonPrimitive(key));
        this.key = key;
    }

    public static Builder keyedContext(String key){
        return new Builder(key);
    }
    public static Builder context(){
        return new Builder();
    }

    public Map<String, JsonElement> getValues() {
        return values;
    }

    public static class Builder{
        private String key;
        private Map<String, JsonElement> values = new HashMap<>();

        public Builder() {}
        public Builder(String key) {
            this.key = key;
        }

        public Builder withValue(String key, String value){
            JsonPrimitive jsonValue = new JsonPrimitive(value);
            this.values.put(key, jsonValue);
            return this;
        }
        public Builder withValue(String key, DateTime value){
            JsonPrimitive jsonValue = new JsonPrimitive(toIso(value));
            this.values.put(key, jsonValue);
            return this;
        }
        public Builder withValue(String key, Number value){
            JsonPrimitive jsonValue = new JsonPrimitive(value);
            this.values.put(key, jsonValue);
            return this;
        }

        public Builder withDateValues(String key, List<DateTime> values){
            JsonArray vals = new JsonArray();
            for (DateTime value : values) {
                vals.add(new JsonPrimitive(toIso(value)));
            }
            this.values.put(key, vals);
            return this;

        }
        public Builder withNumberValues(String key, List<Number> values) {
            JsonArray vals = new JsonArray();
            for (Number value : values) {
                vals.add(new JsonPrimitive(value));
            }
            this.values.put(key, vals);
            return this;
        }

        public Builder withValues(String key, List<String> values){
            JsonArray vals = new JsonArray();
            for (String value : values) {
                vals.add(new JsonPrimitive(value));
            }
            this.values.put(key, vals);
            return this;
        }

        public FeatureflowContext build(){
            if(key==null)key = "anonymous";
            FeatureflowContext context = new FeatureflowContext(key);
            context.values = values;
            return context;
        }

        protected static String toIso(DateTime date) {
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            String str = fmt.print(date);
            return str;
        }
        protected static DateTime fromIso(String isoDate) {
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            DateTime dt = fmt.parseDateTime(isoDate);
            return dt;
        }
    }
}
