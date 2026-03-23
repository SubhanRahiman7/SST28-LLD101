package com.example.pen;

/**
 * Pen context that delegates start/write/close behavior to state objects.
 */
public final class Pen {
    private final InkReservoir ink;
    private final Paper paper;
    private PenState state;

    public Pen(InkReservoir ink, Paper paper) {
        if (ink == null) {
            throw new IllegalArgumentException("ink cannot be null");
        }
        if (paper == null) {
            throw new IllegalArgumentException("paper cannot be null");
        }
        this.ink = ink;
        this.paper = paper;
        this.state = new ClosedState();
    }

    // Delegation entry points
    public void start() {
        state.start(this);
    }

    public void write(String text) {
        state.write(this, text);
    }

    public void close() {
        state.close(this);
    }

    // Refill is allowed in any state.
    public void refill(int amountUnits) {
        ink.refill(amountUnits);
    }

    public void refillFull() {
        ink.refillFull();
    }

    // Package-private helpers for state objects (kept out of public API).
    InkReservoir ink() {
        return ink;
    }

    Paper paper() {
        return paper;
    }

    void setState(PenState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        this.state = state;
    }
}

