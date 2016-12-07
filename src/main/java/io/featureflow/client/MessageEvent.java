package io.featureflow.client;

import java.net.URI;

/**
 * Created by oliver on 6/06/2016.
 */
public class MessageEvent {
    private final String data;
    private final String lastEventId;
    private final URI origin;

    public MessageEvent(String data, String lastEventId, URI origin) {
        this.data = data;
        this.lastEventId = lastEventId;
        this.origin = origin;
    }

    public MessageEvent(String data) {
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