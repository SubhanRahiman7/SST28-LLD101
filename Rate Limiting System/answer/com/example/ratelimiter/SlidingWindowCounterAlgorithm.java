package com.example.ratelimiter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public final class SlidingWindowCounterAlgorithm implements RateLimitingAlgorithm {
    private static final class SlidingState {
        final Deque<Long> timestamps = new ArrayDeque<>();
    }

    private final ConcurrentHashMap<String, SlidingState> states = new ConcurrentHashMap<>();

    @Override
    public Decision allow(RateLimitKey key, LimitRule rule, long nowMs) {
        String stateKey = key.asCompositeKey() + "|" + rule.ruleName() + "|" + rule.windowMs() + "|" + rule.maxRequests();
        SlidingState st = states.computeIfAbsent(stateKey, k -> new SlidingState());

        synchronized (st) {
            long windowStartInclusive = nowMs - rule.windowMs();
            while (!st.timestamps.isEmpty() && st.timestamps.peekFirst() <= windowStartInclusive) {
                st.timestamps.pollFirst();
            }

            if (st.timestamps.size() < rule.maxRequests()) {
                st.timestamps.addLast(nowMs);
                return Decision.allowed();
            }

            long oldest = st.timestamps.peekFirst();
            long retryAfter = (oldest + rule.windowMs()) - nowMs;
            return Decision.denied(retryAfter, "SlidingWindow limit exceeded");
        }
    }

    @Override
    public String name() {
        return "SlidingWindowCounter";
    }
}

