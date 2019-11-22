package io.featureflow.client.model;

import io.featureflow.client.FeatureflowUser;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * Created by oliver.oldfieldhodge on 3/1/17.
 */
public class Event {
    public static final String EVALUATE_EVENT = "evaluate";
    public static final String GOAL_EVENT = "goal";

    FeatureflowUser user;
    String featureKey;
    String goalKey;
    String type;
    DateTime timestamp;

    String evaluatedVariant;
    String expectedVariant;
    Map<String, String> evaluatedVariants;

    public Event(String featureKey, String type, FeatureflowUser user, String evaluatedVariant, String expectedVariant) {
        this.featureKey = featureKey;
        this.type = type;
        this.timestamp = new DateTime();
        this.user = user;
        this.evaluatedVariant = evaluatedVariant;
        this.expectedVariant = expectedVariant;
    }

    public Event(String goalKey, FeatureflowUser user, Map<String, String> evaluatedVariants) {
        this.type = GOAL_EVENT;
        this.goalKey = goalKey;
        this.timestamp = new DateTime();
        this.user = user;
        this.evaluatedVariants = evaluatedVariants;
    }

    @Override
    public String toString() {
        return "Event{" +
                "user=" + user.getId() +
                ", featureKey='" + featureKey + '\'' +
                ", goalKey='" + goalKey + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public String getGoalKey() {
        return goalKey;
    }

    public void setGoalKey(String goalKey) {
        this.goalKey = goalKey;
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

    public String getEvaluatedVariant() {
        return evaluatedVariant;
    }

    public void setEvaluatedVariant(String evaluatedVariant) {
        this.evaluatedVariant = evaluatedVariant;
    }

    public Map<String, String> getEvaluatedVariants() {
        return evaluatedVariants;
    }

    public void setEvaluatedVariants(Map<String, String> evaluatedVariants) {
        this.evaluatedVariants = evaluatedVariants;
    }
}
