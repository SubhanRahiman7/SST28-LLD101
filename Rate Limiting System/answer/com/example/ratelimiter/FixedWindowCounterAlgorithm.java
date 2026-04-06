package com.example.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class FixedWindowCounterAlgorithm implements RateLimitingAlgorithm {
    private static final class WindowState {
        volatile long windowStartMs;
        final AtomicInteger count = new AtomicInteger(0);
    }

    private final ConcurrentHashMap<String, WindowState> states = new ConcurrentHashMap<>();

    @Override
    public Decision allow(RateLimitKey key, LimitRule rule, long nowMs) {
        String stateKey = key.asCompositeKey() + "|" + rule.ruleName() + "|" + rule.windowMs() + "|" + rule.maxRequests();
        WindowState st = states.computeIfAbsent(stateKey, k -> {
            WindowState ws = new WindowState();
            ws.windowStartMs = nowMs - (nowMs % rule.windowMs());
            return ws;
        });

        synchronized (st) {
            long currentWindowStart = nowMs - (nowMs % rule.windowMs());
            if (st.windowStartMs != currentWindowStart) {
                st.windowStartMs = currentWindowStart;
                st.count.set(0);
            }
            int next = st.count.incrementAndGet();
            if (next <= rule.maxRequests()) {
                return Decision.allowed();
            }
            long retryAfter = (st.windowStartMs + rule.windowMs()) - nowMs;
            return Decision.denied(retryAfter, "FixedWindow limit exceeded");
        }
    }

    @Override
    public String name() {
        return "FixedWindowCounter";
    }
}

