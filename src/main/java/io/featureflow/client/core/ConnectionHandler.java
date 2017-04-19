package io.featureflow.client.core;

/**
 * Created by oliver on 6/06/2016.
 */
public interface ConnectionHandler {
    void setReconnectionTimeMillis(long reconnectionTimeMillis);
    void setLastEventId(String lastEventId);
}
