package com.example.parkinglot;

public final class ParkingSlot {
    private final int slotNumber;
    private final SlotType slotType;
    private final int levelIndex;
    private boolean available;

    public ParkingSlot(int slotNumber, SlotType slotType, int levelIndex) {
        if (slotNumber <= 0) {
            throw new IllegalArgumentException("slotNumber must be > 0");
        }
        if (slotType == null) {
            throw new IllegalArgumentException("slotType cannot be null");
        }
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.levelIndex = levelIndex;
        this.available = true;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public boolean isAvailable() {
        return available;
    }

    public void allocate() {
        if (!available) {
            throw new IllegalStateException("Slot " + slotNumber + " is already occupied");
        }
        available = false;
    }

    public void free() {
        available = true;
    }
}

