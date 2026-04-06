package com.example.ratelimiter;

public interface RateLimitingAlgorithm {
    Decision allow(RateLimitKey key, LimitRule rule, long nowMs);

    String name();
}

