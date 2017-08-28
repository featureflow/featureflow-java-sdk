package io.featureflow.client.model;

import io.featureflow.client.FeatureflowContext;
import io.featureflow.client.FeatureflowUser;
import org.joda.time.DateTime;

/**
 * Created by oliver.oldfieldhodge on 3/1/17.
 */
public class Event {
    public static final String EVALUATE_EVENT = "evaluate";
    public static final String GOAL_EVENT = "goal";
    private final FeatureflowUser user;

    String featureKey;
    String type;
    DateTime timestamp;

    FeatureflowContext context;
    String evaluatedVariant;
    String expectedVariant;


    public Event(String featureKey, String type, FeatureflowUser user, String evaluatedVariant, String expectedVariant) {
        this.featureKey = featureKey;
        this.type = type;
        this.timestamp = new DateTime();
        this.user = user;
        this.evaluatedVariant = evaluatedVariant;
        this.expectedVariant = expectedVariant;
    }


    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    public FeatureflowContext getContext() {
        return context;
    }

    public void setContext(FeatureflowContext context) {
        this.context = context;
    }

    public String getEvaluatedVariant() {
        return evaluatedVariant;
    }

    public void setEvaluatedVariant(String evaluatedVariant) {
        this.evaluatedVariant = evaluatedVariant;
    }
}
