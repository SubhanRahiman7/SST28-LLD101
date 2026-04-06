package com.example.distributedcache;

import java.util.Objects;

/**
 * Placeholder MRU policy (most recently used evicted first). Extend with full bookkeeping when needed.
 */
public final class MruEvictionPolicy implements EvictionPolicy {
    private String mostRecent;

    @Override
    public void onAccess(String key) {
        mostRecent = Objects.requireNonNull(key, "key");
    }

    @Override
    public void onPut(String key, boolean wasNew) {
        mostRecent = Objects.requireNonNull(key, "key");
    }

    @Override
    public void onRemove(String key) {
        if (key.equals(mostRecent)) {
            mostRecent = null;
        }
    }

    @Override
    public String pickEvictionCandidate() {
        return mostRecent;
    }
}
