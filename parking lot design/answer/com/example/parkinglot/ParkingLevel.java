package com.example.parkinglot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ParkingLevel {
    private final int levelIndex;
    private final List<ParkingSlot> slots;

    public ParkingLevel(int levelIndex, List<ParkingSlot> slots) {
        if (levelIndex < 0) {
            throw new IllegalArgumentException("levelIndex cannot be negative");
        }
        if (slots == null) {
            throw new IllegalArgumentException("slots cannot be null");
        }
        this.levelIndex = levelIndex;
        this.slots = new ArrayList<>(slots);
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public List<ParkingSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }
}

