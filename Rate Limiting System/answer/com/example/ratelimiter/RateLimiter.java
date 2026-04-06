package com.example.ratelimiter;

import java.util.Objects;

public final class RateLimiter {
    private final RateLimitingAlgorithm algorithm;

    public RateLimiter(RateLimitingAlgorithm algorithm) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
    }

    public Decision allow(RateLimitKey key, LimitRule rule) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(rule, "rule");
        return algorithm.allow(key, rule, System.currentTimeMillis());
    }

    public String algorithmName() {
        return algorithm.name();
    }
}

