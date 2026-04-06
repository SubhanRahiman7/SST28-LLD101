package com.example.ratelimiter;

public final class Decision {
    private final DecisionStatus status;
    private final long retryAfterMs;
    private final String reason;

    public Decision(DecisionStatus status, long retryAfterMs, String reason) {
        this.status = status;
        this.retryAfterMs = Math.max(0, retryAfterMs);
        this.reason = reason == null ? "" : reason;
    }

    public static Decision allowed() {
        return new Decision(DecisionStatus.ALLOWED, 0, "");
    }

    public static Decision denied(long retryAfterMs, String reason) {
        return new Decision(DecisionStatus.DENIED, retryAfterMs, reason);
    }

    public DecisionStatus status() {
        return status;
    }

    public long retryAfterMs() {
        return retryAfterMs;
    }

    public String reason() {
        return reason;
    }

    @Override
    public String toString() {
        return "Decision{" +
                "status=" + status +
                ", retryAfterMs=" + retryAfterMs +
                ", reason='" + reason + '\'' +
                '}';
    }
}

