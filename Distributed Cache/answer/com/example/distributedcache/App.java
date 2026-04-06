package com.example.distributedcache;

/**
 * Demo: modulo routing, LRU per node, read-through get, write-through put.
 */
public final class App {
    public static void main(String[] args) {
        DatabaseBackend db = new InMemoryDatabase();
        db.put("seed", "from-db");

        DistributedCache cache = new DistributedCache(
                3,
                2,
                new ModuloDistributionStrategy(),
                db,
                EvictionPolicies.lru()
        );

        System.out.println("nodes=" + cache.nodeCount());

        // Miss then read-through from DB
        System.out.println("get seed (expect cache miss -> DB): " + cache.get("seed"));

        // Hit
        System.out.println("get seed (expect hit): " + cache.get("seed"));

        // Single-node demo: capacity 2 => third put evicts LRU (oldest of a,b)
        DistributedCache singleNode = new DistributedCache(
                1,
                2,
                new ModuloDistributionStrategy(),
                new InMemoryDatabase(),
                EvictionPolicies.lru()
        );
        singleNode.put("a", "1");
        singleNode.put("b", "2");
        singleNode.put("c", "3");
        // "a" is LRU-evicted from the cache shard, but write-through put stored it in DB, so read-through still returns it.
        System.out.println("LRU demo (1 node, cap 2): get a (evicted from cache; may reload via DB) -> " + singleNode.get("a"));
        System.out.println("LRU demo: get b (still cached) -> " + singleNode.get("b"));

        // Consistent hashing skeleton (ring registration)
        DistributedCache chCache = new DistributedCache(
                4,
                10,
                new ConsistentHashingDistributionStrategy(8),
                new InMemoryDatabase(),
                EvictionPolicies.lru()
        );
        chCache.put("x", "y");
        System.out.println("CH get: " + chCache.get("x"));
    }
}
