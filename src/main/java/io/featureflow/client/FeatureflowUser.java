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
public class FeatureflowUser {


    public static final String ANONYMOUS = "anonymous";
    private String id;
    private String bucketKey = null;
    private boolean saveUser = true;
    private Map<String, JsonElement> attributes = new HashMap<>();
    private Map<String, JsonElement> sessionAttributes = new HashMap<>(); //transient - session specific attributes

    public static final String FEATUREFLOW_USER_ID = "featureflow.user.id";
    public static final String FEATUREFLOW_DATE = "featureflow.date";
    public static final String FEATUREFLOW_HOUROFDAY = "featureflow.hourofday";

    public FeatureflowUser() {
        this.id = ANONYMOUS;
        this.bucketKey = null;
        this.saveUser = false; //do not save anon data by default
    }

    public FeatureflowUser(String id) {
        this.id = id;
    }

    /**
     * The bucket key is used specifically for percentage rollouts,
     * it is the key by default however you may wish to set it specifically to handle a consistent
     * experience if the key varies (such as a user logging in)
     * @param bucketKey
     * @return
     */
    public FeatureflowUser withBucketKey(String bucketKey){
        this.bucketKey = bucketKey;
        return this;
    }

    /**
     * Whether we should persist this context information to assist in lookup later
     * @param save
     * @return
     */
    public FeatureflowUser saveUser(boolean save){
        this.saveUser = save;
        return this;
    }

    public FeatureflowUser setAttributes(Map<String, JsonElement> attributes) {
        this.attributes = attributes; return this;
    }
    public FeatureflowUser setSessionAttributes(Map<String, JsonElement> sessionAttributes) {
        this.sessionAttributes = sessionAttributes;
        return this;
    }


    public FeatureflowUser withAttribute(String key, String value) {
        JsonPrimitive jsonValue = new JsonPrimitive(value);
        this.attributes.put(key, jsonValue);
        return this;
    }
    public FeatureflowUser withAttribute(String key, boolean value) {
        JsonPrimitive jsonValue = new JsonPrimitive(value);
        this.attributes.put(key, jsonValue);
        return this;
    }

    public FeatureflowUser withAttribute(String key, DateTime value) {
        JsonPrimitive jsonValue = new JsonPrimitive(toIso(value));
        this.attributes.put(key, jsonValue);
        return this;
    }

    public FeatureflowUser withAttribute(String key, Number value) {
        JsonPrimitive jsonValue = new JsonPrimitive(value);
        this.attributes.put(key, jsonValue);
        return this;
    }

    public FeatureflowUser withDateAttributes(String key, List<DateTime> values) {
        JsonArray vals = new JsonArray();
        for (DateTime value : values) {
            vals.add(new JsonPrimitive(toIso(value)));
        }
        this.attributes.put(key, vals);
        return this;

    }

    public FeatureflowUser withNumberAttributes(String key, List<Number> values) {
        JsonArray vals = new JsonArray();
        for (Number value : values) {
            vals.add(new JsonPrimitive(value));
        }
        this.attributes.put(key, vals);
        return this;
    }

    public FeatureflowUser withStringAttributes(String key, List<String> values) {
        JsonArray vals = new JsonArray();
        for (String value : values) {
            vals.add(new JsonPrimitive(value));
        }
        this.attributes.put(key, vals);
        return this;
    }

    public FeatureflowUser withAttributes(Map<String, JsonElement> values) {
        this.attributes.putAll(values);
        return this;
    }


    /*Session attributes*/



    public FeatureflowUser withSessionAttribute(String key, String value) {
        JsonPrimitive jsonValue = new JsonPrimitive(value);
        this.sessionAttributes.put(key, jsonValue);
        return this;
    }

    public FeatureflowUser withSessionAttribute(String key, DateTime value) {
        JsonPrimitive jsonValue = new JsonPrimitive(toIso(value));
        this.sessionAttributes.put(key, jsonValue);
        return this;
    }

    public FeatureflowUser withSessionAttribute(String key, Number value) {
        JsonPrimitive jsonValue = new JsonPrimitive(value);
        this.sessionAttributes.put(key, jsonValue);
        return this;
    }

    public FeatureflowUser withSessionDateAttributes(String key, List<DateTime> values) {
        JsonArray vals = new JsonArray();
        for (DateTime value : values) {
            vals.add(new JsonPrimitive(toIso(value)));
        }
        this.sessionAttributes.put(key, vals);
        return this;

    }

    public FeatureflowUser withSessionNumberAttributes(String key, List<Number> values) {
        JsonArray vals = new JsonArray();
        for (Number value : values) {
            vals.add(new JsonPrimitive(value));
        }
        this.sessionAttributes.put(key, vals);
        return this;
    }

    public FeatureflowUser withSessionStringAttributes(String key, List<String> values) {
        JsonArray vals = new JsonArray();
        for (String value : values) {
            vals.add(new JsonPrimitive(value));
        }
        this.sessionAttributes.put(key, vals);
        return this;
    }

    public FeatureflowUser withSessionAttributes(Map<String, JsonElement> values) {
        this.sessionAttributes.putAll(values);
        return this;
    }




    public String getId() {
        return id;
    }
    public String getBucketKey() {
        return bucketKey;
    }
    public boolean isSaveUser() {
        return saveUser;
    }
    public Map<String, JsonElement> getAttributes() {
        return attributes;
    }

    public Map<String, JsonElement> getSessionAttributes() {
        return sessionAttributes;
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
