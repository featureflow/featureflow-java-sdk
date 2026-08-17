package io.featureflow.client.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.featureflow.client.FeatureflowConfig;
import io.featureflow.client.FeatureflowUser;
import io.featureflow.client.model.Event;

/**
 * The events client summarises evaluate events client-side (one entry per
 * (featureKey, evaluatedVariant) with an impression count) and sends goal/track events
 * raw, flushing both on a reconfigurable interval. It honours server-driven SDK config
 * ({@code eventsEnabled}/{@code mode}/{@code flushIntervalSeconds}) and responds to
 * server signals: 401/403 permanently disables sending, 429 backs off and requeues.
 * Mirrors featureflow-node-sdk's EventsClient.js, the reference implementation of the
 * contract in featureflow-sdk-testbed/SDK-CONFIG.md.
 */
public class EventsClientImpl implements EventsClient {
    private static final Logger logger = LoggerFactory.getLogger(EventsClientImpl.class);
    private static final Gson GSON = new Gson();
    private static final String KEY_SEPARATOR = "";

    private static final int DEFAULT_SEND_INTERVAL = 60;
    private static final int MIN_SEND_INTERVAL = 1;
    private static final int MAX_SEND_INTERVAL = 3600;
    private static final int GOALS_CAPACITY = 10000;
    private static final int USER_CACHE_CAPACITY = 1000;
    private static final int TRACKED_USER_CACHE_CAPACITY = 10000;
    private static final long DEFAULT_RETRY_AFTER_SECONDS = 60;

    private final RestClient restClient;
    private final long creationTime;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;

    private Map<String, SummaryEntry> summaries = new LinkedHashMap<>();
    private List<Event> goals = new ArrayList<>();
    private final LinkedHashMap<String, Boolean> seenUserIds = new LinkedHashMap<>();
    private final LinkedHashMap<String, Boolean> seenTrackedUserFlags = new LinkedHashMap<>();
    // Test-support only: the most recently observed trackEvents flag per feature key.
    private final Map<String, Boolean> lastTrackEventsByFeature = new LinkedHashMap<>();
    private int fullModeCounter = 0;

    private volatile boolean disabled;
    private volatile boolean suspended = false;
    private volatile String mode = "summary";
    private volatile long backoffUntil = 0;
    private volatile int sendIntervalSeconds = DEFAULT_SEND_INTERVAL;
    private volatile int summaryCapacity = 10000;

    public EventsClientImpl(FeatureflowConfig config, RestClient restClient) {
        this(config, restClient, false);
    }

    public EventsClientImpl(FeatureflowConfig config, RestClient restClient, boolean disabled) {
        this.restClient = restClient;
        this.disabled = disabled;
        this.creationTime = System.currentTimeMillis();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "featureflow-events-client");
            t.setDaemon(true);
            return t;
        });
        scheduleFlush();
    }

    private void scheduleFlush() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = scheduler.scheduleAtFixedRate(this::sendQueue, sendIntervalSeconds, sendIntervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void evaluateEvent(String featureKey, String evaluatedVariant, FeatureflowUser user, boolean trackEvents) {
        if (disabled || suspended || "off".equals(mode)) {
            return;
        }
        lastTrackEventsByFeature.put(featureKey, trackEvents);
        String key = "full".equals(mode) ? String.valueOf(fullModeCounter++) : featureKey + KEY_SEPARATOR + evaluatedVariant;
        SummaryEntry entry = summaries.get(key);
        if (entry == null) {
            if (summaries.size() >= summaryCapacity) {
                logger.debug("Summary capacity of {} feature/variant entries exceeded. New entries will be dropped until the summary is flushed.", summaryCapacity);
                return;
            }
            entry = new SummaryEntry(featureKey, evaluatedVariant);
            summaries.put(key, entry);
        }
        entry.impressions++;
        attachUser(entry, user, trackEvents);
    }

    // Attach each distinct user to at most one summary entry per flush interval, so the
    // server still sees every user's attributes without the payload repeating the user
    // on every evaluation. Flags with trackEvents (experiment exposure fidelity) instead
    // attach each distinct user once per flag per interval.
    private void attachUser(SummaryEntry entry, FeatureflowUser user, boolean trackEvents) {
        if (user == null || user.getId() == null) {
            return;
        }
        if ("full".equals(mode)) {
            entry.users.add(user);
            return;
        }
        if (trackEvents) {
            String trackedKey = entry.featureKey + KEY_SEPARATOR + user.getId();
            if (seenTrackedUserFlags.containsKey(trackedKey)) {
                return;
            }
            evictOldestIfFull(seenTrackedUserFlags, TRACKED_USER_CACHE_CAPACITY);
            seenTrackedUserFlags.put(trackedKey, Boolean.TRUE);
            entry.users.add(user);
            return;
        }
        if (seenUserIds.containsKey(user.getId())) {
            return;
        }
        evictOldestIfFull(seenUserIds, USER_CACHE_CAPACITY);
        seenUserIds.put(user.getId(), Boolean.TRUE);
        entry.users.add(user);
    }

    private static void evictOldestIfFull(LinkedHashMap<String, Boolean> map, int capacity) {
        if (map.size() >= capacity) {
            Iterator<String> it = map.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    @Override
    public synchronized void trackEvent(String goalKey, FeatureflowUser user, Object details) {
        if (disabled || suspended || "off".equals(mode) || goalKey == null) {
            return;
        }
        if (goals.size() >= GOALS_CAPACITY) {
            logger.debug("Goal event capacity of {} exceeded. Goals will be dropped until the queue is flushed.", GOALS_CAPACITY);
            return;
        }
        Double value = null;
        Map<String, Object> data = null;
        if (details instanceof Number) {
            value = ((Number) details).doubleValue();
        } else if (details instanceof Map) {
            Map<?, ?> detailsMap = (Map<?, ?>) details;
            Object v = detailsMap.get("value");
            if (v instanceof Number) {
                value = ((Number) v).doubleValue();
            }
            Map<String, Object> customData = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : detailsMap.entrySet()) {
                if (!"value".equals(e.getKey())) {
                    customData.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            if (!customData.isEmpty()) {
                data = customData;
            }
        }
        goals.add(Event.goal(goalKey, user, value, data));
    }

    @Override
    public synchronized void applyServerConfig(Map<String, Object> config) {
        if (disabled || config == null) {
            return;
        }
        Object eventsEnabled = config.get("eventsEnabled");
        if (eventsEnabled instanceof Boolean) {
            setSuspended(!((Boolean) eventsEnabled));
        }
        Object modeValue = config.get("mode");
        if (modeValue instanceof String && ("summary".equals(modeValue) || "full".equals(modeValue) || "off".equals(modeValue))) {
            this.mode = (String) modeValue;
        }
        Object flushIntervalSeconds = config.get("flushIntervalSeconds");
        if (flushIntervalSeconds instanceof Number) {
            double seconds = ((Number) flushIntervalSeconds).doubleValue();
            if (seconds >= MIN_SEND_INTERVAL && seconds <= MAX_SEND_INTERVAL) {
                setSendInterval((int) seconds);
            }
        }
    }

    private void setSuspended(boolean suspended) {
        if (suspended && !this.suspended) {
            logger.debug("Event sending suspended by server config.");
            summaries = new LinkedHashMap<>();
            goals = new ArrayList<>();
            seenUserIds.clear();
            seenTrackedUserFlags.clear();
        }
        this.suspended = suspended;
    }

    private void setSendInterval(int seconds) {
        if (seconds == sendIntervalSeconds) {
            return;
        }
        logger.debug("Event flush interval changed by server config: {}s -> {}s", sendIntervalSeconds, seconds);
        sendIntervalSeconds = seconds;
        scheduleFlush();
    }

    @Override
    public synchronized void sendQueue() {
        if (disabled || suspended || "off".equals(mode)) {
            return;
        }
        if (summaries.isEmpty() && goals.isEmpty()) {
            return;
        }
        if (System.currentTimeMillis() < backoffUntil) {
            logger.debug("Event sending is backing off after a 429 response. Retaining {} summarised events and {} goals.", summaries.size(), goals.size());
            return;
        }

        Map<String, SummaryEntry> sendSummaries = summaries;
        List<Event> sendGoals = goals;
        summaries = new LinkedHashMap<>();
        goals = new ArrayList<>();
        seenUserIds.clear();
        seenTrackedUserFlags.clear();

        List<Event> batch = buildBatch(sendSummaries);
        batch.addAll(sendGoals);

        EventsPostResult result;
        try {
            result = restClient.postEvents(batch);
        } catch (Exception e) {
            logger.error("Exception posting events", e);
            result = null;
        }
        if (result == null) {
            // Network error: fire-and-forget, the batch is dropped (matches node's client).
            return;
        }
        if (result.statusCode == 401 || result.statusCode == 403) {
            logger.warn("Received {} sending events. The API key is not authorized — disabling event sending for the lifetime of this client.", result.statusCode);
            disable();
            return;
        }
        if (result.statusCode == 429) {
            backoffUntil = System.currentTimeMillis() + parseRetryAfter(result.retryAfterHeader) * 1000L;
            requeue(sendSummaries);
            List<Event> mergedGoals = new ArrayList<>(sendGoals);
            mergedGoals.addAll(goals);
            if (mergedGoals.size() > GOALS_CAPACITY) {
                mergedGoals = new ArrayList<>(mergedGoals.subList(0, GOALS_CAPACITY));
            }
            goals = mergedGoals;
            return;
        }
        if (result.statusCode == 200 && result.body != null && !result.body.trim().isEmpty()) {
            applyServerConfig(parseConfig(result.body));
        }
    }

    // One event per (featureKey, variant) with summed impressions. A wire event carries
    // at most one user, so entries with more than one attached user emit one extra
    // single-impression event per additional user, keeping the impression total exact.
    private List<Event> buildBatch(Map<String, SummaryEntry> summaries) {
        List<Event> events = new ArrayList<>();
        for (SummaryEntry entry : summaries.values()) {
            int extraUsers = Math.max(entry.users.size() - 1, 0);
            FeatureflowUser firstUser = entry.users.isEmpty() ? null : entry.users.get(0);
            events.add(Event.evaluate(entry.featureKey, entry.evaluatedVariant, entry.impressions - extraUsers, firstUser));
            for (int i = 1; i < entry.users.size(); i++) {
                events.add(Event.evaluate(entry.featureKey, entry.evaluatedVariant, 1, entry.users.get(i)));
            }
        }
        return events;
    }

    // Merge a 429-rejected batch back over anything summarised since, dropping the
    // newest entries if the combined summary exceeds capacity.
    private void requeue(Map<String, SummaryEntry> sendSummaries) {
        for (Map.Entry<String, SummaryEntry> e : summaries.entrySet()) {
            SummaryEntry rejected = sendSummaries.get(e.getKey());
            SummaryEntry entry = e.getValue();
            if (rejected != null) {
                rejected.impressions += entry.impressions;
                for (FeatureflowUser u : entry.users) {
                    boolean present = rejected.users.stream().anyMatch(ru -> ru.getId().equals(u.getId()));
                    if (!present) {
                        rejected.users.add(u);
                    }
                }
            } else if (sendSummaries.size() < summaryCapacity) {
                sendSummaries.put(e.getKey(), entry);
            }
        }
        summaries = sendSummaries;
    }

    private long parseRetryAfter(String header) {
        if (header != null) {
            try {
                long seconds = Long.parseLong(header.trim());
                if (seconds > 0) {
                    return seconds;
                }
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return DEFAULT_RETRY_AFTER_SECONDS;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String json) {
        try {
            return GSON.fromJson(json, Map.class);
        } catch (Exception e) {
            logger.debug("ignoring malformed SDK config body: {}", json);
            return null;
        }
    }

    private void disable() {
        disabled = true;
        summaries = new LinkedHashMap<>();
        goals = new ArrayList<>();
        seenUserIds.clear();
        seenTrackedUserFlags.clear();
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    @Override
    public void close() throws IOException {
        // Only send queued events if the client has existed for more than 5 minutes.
        // This prevents accidental DDOS if the user is managing the featureflow client
        // singleton incorrectly.
        if (System.currentTimeMillis() - creationTime > 5 * 60 * 1000L) {
            sendQueue();
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduler.shutdownNow();
    }

    // --- Test-support accessors (used by cucumber step defs) ---

    public void setSummaryCapacity(int capacity) {
        this.summaryCapacity = capacity;
    }

    public synchronized int getPendingSummaryCount() {
        return summaries.size();
    }

    public synchronized List<SummarySnapshot> getSummarySnapshot() {
        List<SummarySnapshot> result = new ArrayList<>();
        for (SummaryEntry e : summaries.values()) {
            List<String> userIds = new ArrayList<>();
            for (FeatureflowUser u : e.users) {
                userIds.add(u.getId());
            }
            result.add(new SummarySnapshot(e.featureKey, e.evaluatedVariant, e.impressions, userIds));
        }
        return result;
    }

    public synchronized int getPendingGoalCount() {
        return goals.size();
    }

    public synchronized List<Event> getPendingGoals() {
        return new ArrayList<>(goals);
    }

    public synchronized Boolean getLastTrackEvents(String featureKey) {
        return lastTrackEventsByFeature.get(featureKey);
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public boolean isBackingOff() {
        return backoffUntil > System.currentTimeMillis();
    }

    public int getSendIntervalSeconds() {
        return sendIntervalSeconds;
    }

    public static final class SummarySnapshot {
        public final String featureKey;
        public final String evaluatedVariant;
        public final int impressions;
        public final List<String> userIds;

        SummarySnapshot(String featureKey, String evaluatedVariant, int impressions, List<String> userIds) {
            this.featureKey = featureKey;
            this.evaluatedVariant = evaluatedVariant;
            this.impressions = impressions;
            this.userIds = userIds;
        }
    }

    private static final class SummaryEntry {
        final String featureKey;
        final String evaluatedVariant;
        int impressions = 0;
        final List<FeatureflowUser> users = new ArrayList<>();

        SummaryEntry(String featureKey, String evaluatedVariant) {
            this.featureKey = featureKey;
            this.evaluatedVariant = evaluatedVariant;
        }
    }
}
