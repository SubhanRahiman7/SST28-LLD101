package com.example.distributedcache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal LFU sketch: frequency counts; evicts lowest frequency (ties: arbitrary key order).
 * Suitable for demonstrating pluggable eviction; production LFU uses more efficient structures.
 */
public final class LfuEvictionPolicy implements EvictionPolicy {
    private final Map<String, Integer> freq = new HashMap<>();

    @Override
    public void onAccess(String key) {
        String k = Objects.requireNonNull(key, "key");
        freq.merge(k, 1, Integer::sum);
    }

    @Override
    public void onPut(String key, boolean wasNew) {
        String k = Objects.requireNonNull(key, "key");
        if (wasNew) {
            freq.putIfAbsent(k, 1);
        } else {
            freq.merge(k, 1, Integer::sum);
        }
    }

    @Override
    public void onRemove(String key) {
        freq.remove(key);
    }

    @Override
    public String pickEvictionCandidate() {
        return freq.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
