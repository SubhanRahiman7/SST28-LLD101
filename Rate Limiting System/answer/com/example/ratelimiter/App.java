package com.example.ratelimiter;

public final class App {
    public static void main(String[] args) throws Exception {
        RateLimitKey tenantT1 = new RateLimitKey("tenant", "T1");
        LimitRule fivePerMinute = new LimitRule(5, 60_000, "5-per-minute");
        ExternalProviderClient externalClient = new MockExternalProviderClient();

        // Business logic is unchanged; only algorithm wiring changes.
        runScenario("Fixed Window", new FixedWindowCounterAlgorithm(), tenantT1, fivePerMinute, externalClient);
        System.out.println("------------------------------------------------");
        runScenario("Sliding Window", new SlidingWindowCounterAlgorithm(), tenantT1, fivePerMinute, externalClient);
    }

    private static void runScenario(
            String label,
            RateLimitingAlgorithm algorithm,
            RateLimitKey key,
            LimitRule rule,
            ExternalProviderClient externalClient
    ) {
        System.out.println(label + " demo");
        BusinessService svc = new BusinessService(new RateLimiter(algorithm), externalClient, rule);

        // 7 requests: two of them do not need external call and should skip limiter.
        boolean[] needsExternal = {true, true, false, true, true, true, false, true, true};
        for (int i = 0; i < needsExternal.length; i++) {
            String reqId = label.replace(" ", "") + "-REQ-" + (i + 1);
            String out = svc.handleRequest(reqId, key, needsExternal[i]);
            System.out.println(out);
        }
    }
}

