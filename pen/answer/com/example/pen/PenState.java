package com.example.pen;

/**
 * Pen behavior varies based on whether it is closed or started.
 */
interface PenState {
    void start(Pen pen);

    void write(Pen pen, String text);

    void close(Pen pen);
}

