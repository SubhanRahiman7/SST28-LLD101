package com.example.distributedcache;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database abstraction (Dependency Inversion). Real systems use JDBC/NoSQL/etc.
 */
public interface DatabaseBackend {
    Optional<String> get(String key);

    void put(String key, String value);
}

/**
 * In-memory stand-in for LLD demo and tests.
 */
final class InMemoryDatabase implements DatabaseBackend {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public void put(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        store.put(key, value);
    }
}
