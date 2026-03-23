package com.example.pen;

/**
 * When the pen is closed, writing is not allowed.
 */
final class ClosedState implements PenState {
    @Override
    public void start(Pen pen) {
        if (pen.ink().isEmpty()) {
            throw new IllegalStateException("Cannot start: no ink available.");
        }
        pen.setState(new StartedState());
    }

    @Override
    public void write(Pen pen, String text) {
        throw new IllegalStateException("Cannot write: pen is closed. Call start() first.");
    }

    @Override
    public void close(Pen pen) {
        // Already closed => no-op.
    }
}

