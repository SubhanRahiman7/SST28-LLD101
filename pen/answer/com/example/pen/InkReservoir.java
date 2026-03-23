package com.example.pen;

/**
 * Stores ink units inside the pen and supports drawing/refilling.
 */
public final class InkReservoir {
    private final int capacityUnits;
    private int currentUnits;

    public InkReservoir(int capacityUnits, int initialUnits) {
        if (capacityUnits <= 0) {
            throw new IllegalArgumentException("capacityUnits must be > 0");
        }
        if (initialUnits < 0) {
            throw new IllegalArgumentException("initialUnits cannot be negative");
        }
        if (initialUnits > capacityUnits) {
            throw new IllegalArgumentException("initialUnits cannot exceed capacityUnits");
        }
        this.capacityUnits = capacityUnits;
        this.currentUnits = initialUnits;
    }

    public boolean isEmpty() {
        return currentUnits <= 0;
    }

    int remainingUnits() {
        return currentUnits;
    }

    public void draw(int units) {
        if (units < 0) {
            throw new IllegalArgumentException("units cannot be negative");
        }
        if (units == 0) {
            return;
        }
        if (currentUnits < units) {
            throw new IllegalStateException("Not enough ink. Needed " + units + " units, but only " + currentUnits + " remain.");
        }
        currentUnits -= units;
    }

    public void refill(int amountUnits) {
        if (amountUnits <= 0) {
            throw new IllegalArgumentException("amountUnits must be > 0");
        }
        currentUnits = Math.min(capacityUnits, currentUnits + amountUnits);
    }

    public void refillFull() {
        currentUnits = capacityUnits;
    }
}

