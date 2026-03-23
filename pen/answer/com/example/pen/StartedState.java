package com.example.pen;

/**
 * When the pen is started, it can write and be closed later.
 */
final class StartedState implements PenState {
    @Override
    public void start(Pen pen) {
        // Already started => no-op.
    }

    @Override
    public void write(Pen pen, String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if (text.isEmpty()) {
            return; // nothing to draw/write
        }

        int requiredUnits = text.length(); // 1 ink unit per character
        pen.ink().draw(requiredUnits);
        pen.paper().write(text);
    }

    @Override
    public void close(Pen pen) {
        pen.setState(new ClosedState());
    }
}

