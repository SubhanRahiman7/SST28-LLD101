package com.example.ratelimiter;

import java.util.Objects;

enum DecisionStatus {
    ALLOWED,
    DENIED
}

final class RateLimitKey {
    private final String keyType; // tenant/customer/api-key/provider etc.
    private final String keyValue;

    public RateLimitKey(String keyType, String keyValue) {
        this.keyType = requireNonBlank(keyType, "keyType");
        this.keyValue = requireNonBlank(keyValue, "keyValue");
    }

    public String keyType() {
        return keyType;
    }

    public String keyValue() {
        return keyValue;
    }

    public String asCompositeKey() {
        return keyType + ":" + keyValue;
    }

    private static String requireNonBlank(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " cannot be null/blank");
        return v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RateLimitKey that)) return false;
        return keyType.equals(that.keyType) && keyValue.equals(that.keyValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyType, keyValue);
    }
}

final class LimitRule {
    private final int maxRequests;
    private final long windowMs;
    private final String ruleName;

    public LimitRule(int maxRequests, long windowMs, String ruleName) {
        if (maxRequests <= 0) throw new IllegalArgumentException("maxRequests must be > 0");
        if (windowMs <= 0) throw new IllegalArgumentException("windowMs must be > 0");
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.ruleName = ruleName == null ? "" : ruleName;
    }

    public int maxRequests() {
        return maxRequests;
    }

    public long windowMs() {
        return windowMs;
    }

    public String ruleName() {
        return ruleName;
    }
}

