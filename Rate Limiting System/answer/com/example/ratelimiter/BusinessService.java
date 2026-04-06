package com.example.ratelimiter;

/**
 * Represents internal business logic where external paid call may happen.
 * Rate limiting is checked only when external call is required.
 */
final class BusinessService {
    private final RateLimiter rateLimiter;
    private final ExternalProviderClient externalProviderClient;
    private final LimitRule rule;

    BusinessService(RateLimiter rateLimiter, ExternalProviderClient externalProviderClient, LimitRule rule) {
        this.rateLimiter = rateLimiter;
        this.externalProviderClient = externalProviderClient;
        this.rule = rule;
    }

    public String handleRequest(String requestId, RateLimitKey key, boolean needsExternalCall) {
        // Internal business logic would run here first.
        if (!needsExternalCall) {
            return "request=" + requestId + " internal-only path; no quota consumed";
        }

        Decision d = rateLimiter.allow(key, rule);
        if (d.status() == DecisionStatus.DENIED) {
            return "request=" + requestId + " denied by " + rateLimiter.algorithmName() +
                    ", retryAfterMs=" + d.retryAfterMs();
        }
        String externalResult = externalProviderClient.callExternalResource(requestId);
        return "request=" + requestId + " allowed by " + rateLimiter.algorithmName() +
                ", externalResult=" + externalResult;
    }
}

interface ExternalProviderClient {
    String callExternalResource(String requestId);
}

final class MockExternalProviderClient implements ExternalProviderClient {
    @Override
    public String callExternalResource(String requestId) {
        return "OK(" + requestId + ")";
    }
}

