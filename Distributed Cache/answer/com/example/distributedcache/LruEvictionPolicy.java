package com.example.distributedcache;

import java.util.LinkedList;
import java.util.Objects;

/**
 * LRU: least recently used key is at the head of the list.
 */
public final class LruEvictionPolicy implements EvictionPolicy {
    private final LinkedList<String> accessOrder = new LinkedList<>();

    @Override
    public void onAccess(String key) {
        Objects.requireNonNull(key, "key");
        accessOrder.remove(key);
        accessOrder.addLast(key);
    }

    @Override
    public void onPut(String key, boolean wasNew) {
        Objects.requireNonNull(key, "key");
        accessOrder.remove(key);
        accessOrder.addLast(key);
    }

    @Override
    public void onRemove(String key) {
        accessOrder.remove(key);
    }

    @Override
    public String pickEvictionCandidate() {
        return accessOrder.isEmpty() ? null : accessOrder.getFirst();
    }
}
