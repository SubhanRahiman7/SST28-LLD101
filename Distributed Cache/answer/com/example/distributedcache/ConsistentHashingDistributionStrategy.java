package com.example.distributedcache;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Minimal consistent-hashing skeleton (ring). Real CH adds virtual nodes + rebalancing hooks.
 */
public final class ConsistentHashingDistributionStrategy implements DistributionStrategy {
    private final ConcurrentSkipListMap<Long, Integer> ring = new ConcurrentSkipListMap<>();
    private final int virtualNodesPerPhysical;

    public ConsistentHashingDistributionStrategy(int virtualNodesPerPhysical) {
        if (virtualNodesPerPhysical <= 0) throw new IllegalArgumentException("virtualNodesPerPhysical must be > 0");
        this.virtualNodesPerPhysical = virtualNodesPerPhysical;
    }

    /** Call once per node after nodes are known (LLD hook for ring setup). */
    public void registerNode(int nodeIndex, int nodeCount) {
        if (nodeIndex < 0 || nodeIndex >= nodeCount) throw new IllegalArgumentException("invalid node index");
        for (int i = 0; i < virtualNodesPerPhysical; i++) {
            long hash = hash64("node-" + nodeIndex + "-v-" + i);
            ring.put(hash, nodeIndex);
        }
    }

    @Override
    public int selectNodeIndex(String key, int nodeCount) {
        if (nodeCount <= 0) throw new IllegalArgumentException("nodeCount must be > 0");
        Objects.requireNonNull(key, "key");
        if (ring.isEmpty()) {
            return new ModuloDistributionStrategy().selectNodeIndex(key, nodeCount);
        }
        long h = hash64(key);
        Map.Entry<Long, Integer> ce = ring.ceilingEntry(h);
        if (ce == null) {
            ce = ring.firstEntry();
        }
        return Optional.ofNullable(ce).map(Map.Entry::getValue).orElse(0);
    }

    private static long hash64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }
}
