package io.featureflow.client.core;

import java.net.URI;

/**
 * Created by oliver on 6/06/2016.
 */
public class EventSourceMessage {
    private final String data;
    private final String lastEventId;
    private final URI origin;

    public EventSourceMessage(String data, String lastEventId, URI origin) {
        this.data = data;
        this.lastEventId = lastEventId;
        this.origin = origin;
    }

    public EventSourceMessage(String data) {
        this(data, (String) null, (URI) null);
    }

    public String getData() {
        return this.data;
    }

    public String getLastEventId() {
        return this.lastEventId;
    }

    public URI getOrigin() {
        return this.origin;
    }


}