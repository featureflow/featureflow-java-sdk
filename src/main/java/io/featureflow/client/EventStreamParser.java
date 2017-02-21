package io.featureflow.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Created by oliver on 6/06/2016.
 * Event source handler based on netty implementation here:
 * https://github.com/michaelklishin/eventsource-netty5/blob/master/src/main/java/io/opensensors/sse/client/impl/EventStreamParser.java
 */
public class EventStreamParser {
    private static final Logger logger = LoggerFactory.getLogger(EventStreamParser.class);
    private static final String DATA = "data";
    private static final String ID = "id";
    private static final String EVENT = "event";
    private static final String RETRY = "retry";

    private static final String DEFAULT_EVENT = "message";
    private static final String EMPTY_STRING = "";
    private static final Pattern DIGITS_ONLY = Pattern.compile("^[\\d]+$");

    private final EventSourceHandler eventSourceHandler;
    private final ConnectionHandler connectionHandler;
    private final URI origin;

    private StringBuffer data = new StringBuffer();
    private String lastEventId;
    private String eventName = DEFAULT_EVENT;

    EventStreamParser(URI origin, EventSourceHandler eventSourceHandler, ConnectionHandler connectionHandler) {
        this.eventSourceHandler = eventSourceHandler;
        this.origin = origin;
        this.connectionHandler = connectionHandler;
    }

    /**
     * Parse a single line up until the /n character
     * @param line
     */
    public void line(String line) {
        logger.debug("Parsing line: " + line);
        int colonIndex;
        if (line.trim().isEmpty()) {
            dispatchEvent();
        } else if (line.startsWith(":")) {
            // ignore
        } else if ((colonIndex = line.indexOf(":")) != -1) {
            String field = line.substring(0, colonIndex);
            String value = line.substring(colonIndex + 1).replaceFirst(" ", EMPTY_STRING);
            processField(field, value);
        } else {
            processField(line.trim(), EMPTY_STRING); // The spec doesn't say we need to trim the line, but I assume that's an oversight.
        }
    }

    /**
     * Derive the data packet headers base on SSE spec and action as required
     * @param field
     * @param value
     */
    private void processField(String field, String value) {
        if (DATA.equals(field)) {
            data.append(value).append("\n");
        } else if (ID.equals(field)) {
            lastEventId = value;
        } else if (EVENT.equals(field)) {
            eventName = value;
        } else if (RETRY.equals(field) && isNumber(value)) {
            connectionHandler.setReconnectionTimeMillis(Long.parseLong(value));
        }
    }

    private boolean isNumber(String value) {
        return DIGITS_ONLY.matcher(value).matches();
    }

    /**
     * Once the SSE parser has encountered a double newline /n/n it will dispatch the payload for processing
     *
     */
    private void dispatchEvent() {
        if (data.length() == 0) {
            return;
        }
        String dataString = data.toString();
        if (dataString.endsWith("\n")) {
            dataString = dataString.substring(0, dataString.length() - 1);
        }
        MessageEvent message = new MessageEvent(dataString, lastEventId, origin);
        connectionHandler.setLastEventId(lastEventId);
        try {
            eventSourceHandler.onMessage(eventName, message);
        } catch (Exception e) {
            eventSourceHandler.onError(e);
        }
        data = new StringBuffer();
        eventName = DEFAULT_EVENT;
    }
}
