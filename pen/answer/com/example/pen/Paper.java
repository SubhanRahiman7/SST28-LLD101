package com.example.pen;

/**
 * The writing surface. In a real system, this could be a stream/file.
 */
public final class Paper {
    private final StringBuilder contents = new StringBuilder();

    public void write(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        contents.append(text);
    }

    public String getContents() {
        return contents.toString();
    }
}

