package com.example.distributedcache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Single-node bounded cache. Delegates eviction algorithm to {@link EvictionPolicy}.
 */
final class LocalCache {
    private final int capacity;
    private final Map<String, String> data = new HashMap<>();
    private final EvictionPolicy eviction;

    LocalCache(int capacity, EvictionPolicy eviction) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
        this.eviction = Objects.requireNonNull(eviction, "eviction");
    }

    Optional<String> get(String key) {
        Objects.requireNonNull(key, "key");
        String v = data.get(key);
        if (v == null) {
            return Optional.empty();
        }
        eviction.onAccess(key);
        return Optional.of(v);
    }

    void put(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        boolean wasNew = !data.containsKey(key);
        data.put(key, value);
        eviction.onPut(key, wasNew);
        evictWhileOverCapacity();
    }

    private void evictWhileOverCapacity() {
        while (data.size() > capacity) {
            String victim = eviction.pickEvictionCandidate();
            if (victim == null || !data.containsKey(victim)) {
                // Fallback: remove arbitrary entry to guarantee progress
                victim = data.keySet().iterator().next();
            }
            data.remove(victim);
            eviction.onRemove(victim);
        }
    }

    int size() {
        return data.size();
    }

    int capacity() {
        return capacity;
    }
}
