package com.example.distributedcache;

/**
 * Strategy: which logical node index should own a key.
 */
public interface DistributionStrategy {
    int selectNodeIndex(String key, int nodeCount);
}
