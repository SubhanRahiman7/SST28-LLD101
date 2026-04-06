package com.example.distributedcache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Facade over N {@link CacheNode}s + {@link DistributionStrategy} + {@link DatabaseBackend}.
 * <p>
 * Write-through put: database first, then cache shard.
 * Read-through get: cache first; on miss load from DB and populate shard.
 */
public final class DistributedCache {
    private final List<CacheNode> nodes;
    private final DistributionStrategy distribution;
    private final DatabaseBackend database;

    public DistributedCache(
            int nodeCount,
            int capacityPerNode,
            DistributionStrategy distribution,
            DatabaseBackend database,
            Supplier<EvictionPolicy> evictionPolicyFactory
    ) {
        if (nodeCount <= 0) throw new IllegalArgumentException("nodeCount must be > 0");
        Objects.requireNonNull(distribution, "distribution");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(evictionPolicyFactory, "evictionPolicyFactory");

        List<CacheNode> built = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            LocalCache local = new LocalCache(capacityPerNode, evictionPolicyFactory.get());
            built.add(new CacheNode(i, local));
        }
        this.nodes = Collections.unmodifiableList(built);
        this.distribution = distribution;
        this.database = database;

        if (distribution instanceof ConsistentHashingDistributionStrategy ch) {
            for (int i = 0; i < nodeCount; i++) {
                ch.registerNode(i, nodeCount);
            }
        }
    }

    public int nodeCount() {
        return nodes.size();
    }

    /**
     * @return value from owning shard, or from DB on miss (then cached).
     */
    public Optional<String> get(String key) {
        Objects.requireNonNull(key, "key");
        CacheNode node = nodeFor(key);
        Optional<String> cached = node.get(key);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<String> fromDb = database.get(key);
        if (fromDb.isEmpty()) {
            return Optional.empty();
        }
        String value = fromDb.get();
        node.put(key, value);
        return Optional.of(value);
    }

    /**
     * Write-through: persists to DB then stores in the shard selected for this key.
     */
    public void put(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        database.put(key, value);
        nodeFor(key).put(key, value);
    }

    private CacheNode nodeFor(String key) {
        int idx = distribution.selectNodeIndex(key, nodes.size());
        return nodes.get(idx);
    }
}
