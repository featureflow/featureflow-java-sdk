package io.featureflow.client.core;

import io.featureflow.client.FeatureflowContext;
import org.joda.time.DateTime;

/**
 * Created by oliver.oldfieldhodge on 3/1/17.
 */
public class Event {
    public String key;
    public String eventId;
    public DateTime timestamp;
    public FeatureflowContext context;

    public Event(String featureKey, String eventId, DateTime timestamp, FeatureflowContext context) {
        this.key = featureKey;
        this.eventId = eventId;
        this.timestamp = new DateTime();
        this.context = context;
    }
}
