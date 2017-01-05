package io.featureflow.client;

import org.joda.time.DateTime;

/**
 * Created by oliver.oldfieldhodge on 3/1/17.
 */
public class Event {
    String key;
    String eventId;
    DateTime timestamp;
    FeatureFlowContext context;

    public Event(String featureKey, String eventId, DateTime timestamp, FeatureFlowContext context) {
        this.key = featureKey;
        this.eventId = eventId;
        this.timestamp = new DateTime();
        this.context = context;
    }
}
