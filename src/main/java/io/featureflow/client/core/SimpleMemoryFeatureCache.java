package io.featureflow.client.core;

import io.featureflow.client.model.FeatureControl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple in Map implementation for local feature caching
 */
public class SimpleMemoryFeatureCache implements FeatureControlCache {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, FeatureControl> featureControls = new HashMap<String, FeatureControl>();
    private volatile boolean initialized = false;

    public void init(Map<String, FeatureControl> featureControls) {
        try {
            lock.writeLock().lock();
            this.featureControls.clear();
            this.featureControls.putAll(featureControls);
            initialized = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public FeatureControl get(String key) {
        try {
            lock.readLock().lock();
            FeatureControl fc =  featureControls.get(key);
            if (fc == null || fc.deleted) {
                return null;
            }
            return fc;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, FeatureControl> getAll() {
        try {
            lock.readLock().lock();
            Map<String, FeatureControl> fc = new HashMap<String, FeatureControl>();

            for (Map.Entry<String, FeatureControl> entry : featureControls.entrySet()) {
                if (!entry.getValue().deleted) {
                    fc.put(entry.getKey(), entry.getValue());
                }
            }
            return fc;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void update(String key, FeatureControl featureControl) {
        try {
            lock.writeLock().lock();
            FeatureControl old = featureControls.get(key);
            if(old!=null && featureControl.variants == null)featureControl.variants = old.variants;
            //if (old == null || old.version < featureControl.version) {
                featureControls.put(key, featureControl);
            //}
        }
        finally {
            lock.writeLock().unlock();
        }
    }
    public void delete(String key) {
        try {
            lock.writeLock().lock();
            featureControls.remove(key);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        return;
    }
}
