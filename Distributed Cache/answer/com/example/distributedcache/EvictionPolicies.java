package com.example.distributedcache;

import java.util.function.Supplier;

/**
 * Factory helpers (Factory method) for eviction policy instances.
 * Each node must get its own policy instance (separate LRU lists, etc.).
 */
public final class EvictionPolicies {
    private EvictionPolicies() {
    }

    public static Supplier<EvictionPolicy> lru() {
        return LruEvictionPolicy::new;
    }

    public static Supplier<EvictionPolicy> lfu() {
        return LfuEvictionPolicy::new;
    }

    public static Supplier<EvictionPolicy> mru() {
        return MruEvictionPolicy::new;
    }
}
