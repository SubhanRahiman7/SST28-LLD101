package com.example.distributedcache;

/**
 * Pluggable eviction per cache node (Strategy). Current: LRU via {@link LruEvictionPolicy}.
 * Future: MRU, LFU — new implementations without changing {@link LocalCache} callers.
 */
public interface EvictionPolicy {
    /** Key was read (counts as use for LRU/LFU). */
    void onAccess(String key);

    /**
     * Key was inserted or updated.
     * @param wasNew true if key was not present before this put
     */
    void onPut(String key, boolean wasNew);

    /** Remove key from policy bookkeeping (after eviction or explicit remove). */
    void onRemove(String key);

    /**
     * @return next key to evict, or null if policy cannot choose (empty / inconsistent)
     */
    String pickEvictionCandidate();
}
