package com.example.elevator;

import java.util.Objects;

enum Direction {
    UP,
    DOWN
}

enum ElevatorState {
    IDLE,
    MOVING_UP,
    MOVING_DOWN,
    MAINTENANCE,
    EMERGENCY
}

enum DoorState {
    OPEN,
    CLOSED
}

final class CallRequest {
    final String requestId;
    final int floor;
    final Direction direction; // UP/DOWN button pressed outside
    final long createdAtMs;
    final int priority; // higher = more important
    final boolean expressPreferred;

    CallRequest(String requestId, int floor, Direction direction, long createdAtMs, int priority, boolean expressPreferred) {
        this.requestId = requireNonBlank(requestId, "requestId");
        this.floor = floor;
        this.direction = Objects.requireNonNull(direction, "direction");
        this.createdAtMs = createdAtMs;
        this.priority = priority;
        this.expressPreferred = expressPreferred;
    }

    private static String requireNonBlank(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " cannot be null/blank");
        return v;
    }
}

final class ElevatorSnapshot {
    final String elevatorId;
    final int currentFloor;
    final ElevatorState state;
    final DoorState doorState;
    final boolean weightExceeded;

    ElevatorSnapshot(String elevatorId, int currentFloor, ElevatorState state, DoorState doorState, boolean weightExceeded) {
        this.elevatorId = elevatorId;
        this.currentFloor = currentFloor;
        this.state = state;
        this.doorState = doorState;
        this.weightExceeded = weightExceeded;
    }
}

