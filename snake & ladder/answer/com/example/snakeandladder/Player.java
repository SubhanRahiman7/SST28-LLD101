package com.example.snakeandladder;

public final class Player {
    private final String name;
    private int position; // 0 means outside the board

    public Player(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        this.name = name;
        this.position = 0;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("position cannot be negative");
        }
        this.position = position;
    }
}

