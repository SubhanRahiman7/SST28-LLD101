package com.example.distributedcache;

import java.util.Objects;
import java.util.Optional;

/**
 * One logical shard of the distributed cache (in-process "node").
 */
final class CacheNode {
    private final int nodeId;
    private final LocalCache cache;
    private final Object lock = new Object();

    CacheNode(int nodeId, LocalCache cache) {
        if (nodeId < 0) throw new IllegalArgumentException("nodeId must be >= 0");
        this.nodeId = nodeId;
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    int nodeId() {
        return nodeId;
    }

    Optional<String> get(String key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return cache.get(key);
        }
    }

    void put(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        synchronized (lock) {
            cache.put(key, value);
        }
    }
}
