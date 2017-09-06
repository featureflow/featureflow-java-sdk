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


    private String key;
    private String bucketKey;
    private Map<String, JsonElement> values = new HashMap<>();

    public static final String FEATUREFLOW_KEY = "featureflow.key";
    public static final String FEATUREFLOW_DATE = "featureflow.date";
    public static final String FEATUREFLOW_HOUROFDAY = "featureflow.hourofday";

    public FeatureflowContext() {
        this.key = "anonymous";
        this.values.put(FEATUREFLOW_KEY, new JsonPrimitive(key));
    }

    public FeatureflowContext(String key) {
        this.key = key;
        this.values.put(FEATUREFLOW_KEY, new JsonPrimitive(key));
        this.bucketKey = key;
    }

    /**
     * The bucket key is used specifically for percentage rollouts,
     * it is the key by default however you may wish to set it specifically to handle a consistent
     * experience if the key varies (such as a user logging in)
     * @param bucketKey
     * @return
     */
    public FeatureflowContext withBucketKey(String bucketKey){
        this.bucketKey = bucketKey;
        return this;
    }

    public Map<String, JsonElement> getValues() {
        return values;
    }

    public FeatureflowContext withValue(String key, String value) {
        JsonPrimitive jsonValue = new JsonPrimitive(value);
        this.values.put(key, jsonValue);
        return this;
    }

    public FeatureflowContext withValue(String key, DateTime value) {
        JsonPrimitive jsonValue = new JsonPrimitive(toIso(value));
        this.values.put(key, jsonValue);
        return this;
    }

    public FeatureflowContext withValue(String key, Number value) {
        JsonPrimitive jsonValue = new JsonPrimitive(value);
        this.values.put(key, jsonValue);
        return this;
    }

    public FeatureflowContext withDateValues(String key, List<DateTime> values) {
        JsonArray vals = new JsonArray();
        for (DateTime value : values) {
            vals.add(new JsonPrimitive(toIso(value)));
        }
        this.values.put(key, vals);
        return this;

    }

    public FeatureflowContext withNumberValues(String key, List<Number> values) {
        JsonArray vals = new JsonArray();
        for (Number value : values) {
            vals.add(new JsonPrimitive(value));
        }
        this.values.put(key, vals);
        return this;
    }

    public FeatureflowContext withStringValues(String key, List<String> values) {
        JsonArray vals = new JsonArray();
        for (String value : values) {
            vals.add(new JsonPrimitive(value));
        }
        this.values.put(key, vals);
        return this;
    }

    public FeatureflowContext withValues(Map<String, JsonElement> values) {
        this.values.putAll(values);
        return this;
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

    public String getKey() {
        return key;
    }
    public String getBucketKey() {
        return bucketKey;
    }

}
