package com.example.distributedcache;

import java.util.Objects;

/**
 * hash(key) % N (stable modulo with non-negative index).
 */
public final class ModuloDistributionStrategy implements DistributionStrategy {
    @Override
    public int selectNodeIndex(String key, int nodeCount) {
        if (nodeCount <= 0) throw new IllegalArgumentException("nodeCount must be > 0");
        Objects.requireNonNull(key, "key");
        int h = key.hashCode();
        return Math.floorMod(h, nodeCount);
    }
}
