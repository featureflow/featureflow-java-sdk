package io.featureflow.client.model;

import io.featureflow.client.FeatureflowUser;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * Wire shape for both event types the SDK sends: a summarised evaluate event
 * (featureKey, evaluatedVariant, impressions, one user) and a raw goal/track event
 * (goalKey, user, optional numeric value, optional custom data). Fields irrelevant to a
 * given event's type are left null and omitted from JSON (Gson skips nulls by default),
 * so the two shapes never bleed into each other on the wire.
 */
public class Event {
    public static final String EVALUATE_EVENT = "evaluate";
    public static final String GOAL_EVENT = "goal";

    private String featureKey;
    private String goalKey;
    private String type;
    private DateTime timestamp;
    private FeatureflowUser user;

    private String evaluatedVariant;
    private Integer impressions;

    private Double value;
    private Map<String, Object> data;

    private Event() {
        this.timestamp = new DateTime();
    }

    public static Event evaluate(String featureKey, String evaluatedVariant, int impressions, FeatureflowUser user) {
        Event event = new Event();
        event.type = EVALUATE_EVENT;
        event.featureKey = featureKey;
        event.evaluatedVariant = evaluatedVariant;
        event.impressions = impressions;
        event.user = user;
        return event;
    }

    public static Event goal(String goalKey, FeatureflowUser user, Double value, Map<String, Object> data) {
        Event event = new Event();
        event.type = GOAL_EVENT;
        event.goalKey = goalKey;
        event.user = user;
        event.value = value;
        event.data = data;
        return event;
    }

    @Override
    public String toString() {
        return "Event{" +
                "user=" + (user == null ? null : user.getId()) +
                ", featureKey='" + featureKey + '\'' +
                ", goalKey='" + goalKey + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public String getGoalKey() {
        return goalKey;
    }

    public String getType() {
        return type;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public FeatureflowUser getUser() {
        return user;
    }

    public String getEvaluatedVariant() {
        return evaluatedVariant;
    }

    public Integer getImpressions() {
        return impressions;
    }

    public void setImpressions(int impressions) {
        this.impressions = impressions;
    }

    public Double getValue() {
        return value;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
