package com.example.elevator;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Elevator car worker + state (in-memory simulation).
 *
 * Concurrency model:
 * - stop sets are protected by `lock`
 * - a single worker thread moves the elevator and checks stop sets frequently
 * - system assigns calls by enqueuing stops into this car
 */
final class ElevatorCar implements Runnable {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition idleOrStopChanged = lock.newCondition();
    private final Condition weightChanged = lock.newCondition();

    final String elevatorId;
    final int weightLimitKg;
    final boolean expressEnabled;

    private volatile ElevatorState state = ElevatorState.IDLE;
    private volatile DoorState doorState = DoorState.CLOSED;

    private int currentFloor;
    private long idleSinceMs;

    private final TreeSet<Integer> upStops = new TreeSet<>();
    private final TreeSet<Integer> downStops = new TreeSet<>();

    private volatile boolean running = true;
    private volatile boolean maintenance = false;
    private volatile boolean emergencyMode = false;

    private double loadKg = 0.0;
    private boolean weightExceeded = false;
    private boolean weightExceededAnnounced = false;

    private volatile Direction servingDirection = Direction.UP;

    private final ElevatorArrivalListener listener;
    private final SoundPlayer soundPlayer;
    private final AnnouncementService announcementService;

    ElevatorCar(
            String elevatorId,
            int weightLimitKg,
            boolean expressEnabled,
            int initialFloor,
            ElevatorArrivalListener listener,
            SoundPlayer soundPlayer,
            AnnouncementService announcementService
    ) {
        this.elevatorId = requireNonBlank(elevatorId, "elevatorId");
        if (weightLimitKg <= 0) throw new IllegalArgumentException("weightLimitKg must be > 0");
        this.weightLimitKg = weightLimitKg;
        this.expressEnabled = expressEnabled;
        this.currentFloor = initialFloor;
        this.idleSinceMs = System.currentTimeMillis();

        this.listener = Objects.requireNonNull(listener, "listener");
        this.soundPlayer = Objects.requireNonNull(soundPlayer, "soundPlayer");
        this.announcementService = Objects.requireNonNull(announcementService, "announcementService");
    }

    static String requireNonBlank(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " cannot be null/blank");
        return v;
    }

    @Override
    public void run() {
        while (running) {
            lock.lock();
            try {
                // Emergency mode overrides everything.
                if (emergencyMode) {
                    state = ElevatorState.EMERGENCY;
                    ensureStopAtFloorLocked(1);
                    if (currentFloor == 1) {
                        doorState = DoorState.OPEN;
                        // Notify listener (if any) that we've arrived at ground.
                        if (upStops.remove(1)) {
                            listener.onElevatorArrived(elevatorId, 1, Direction.UP);
                        } else if (downStops.remove(1)) {
                            listener.onElevatorArrived(elevatorId, 1, Direction.DOWN);
                        }
                        idleOrStopChanged.awaitNanos(30_000_000L); // ~30ms
                        continue;
                    }

                    servingDirection = currentFloor > 1 ? Direction.DOWN : Direction.UP;
                    moveTowardsNextStopLocked();
                    idleOrStopChanged.awaitNanos(30_000_000L); // ~30ms
                    continue;
                }

                // Maintenance overrides everything.
                if (maintenance) {
                    state = ElevatorState.MAINTENANCE;
                    doorState = DoorState.CLOSED;
                    idleSinceMs = System.currentTimeMillis();
                    idleOrStopChanged.awaitNanos(50_000_000L);
                    continue;
                }

                // Weight check: do not move, keep doors open.
                updateWeightExceededLocked();
                if (weightExceeded) {
                    state = ElevatorState.IDLE;
                    doorState = DoorState.OPEN;
                    // Wait for weight to be corrected or for emergency/maintenance.
                    weightChanged.awaitNanos(50_000_000L);
                    continue;
                }

                // If no stops pending, go idle.
                boolean hasAnyStop = !upStops.isEmpty() || !downStops.isEmpty();
                if (!hasAnyStop) {
                    state = ElevatorState.IDLE;
                    doorState = doorState == DoorState.OPEN ? DoorState.OPEN : DoorState.CLOSED;
                    idleSinceMs = System.currentTimeMillis();
                    idleOrStopChanged.awaitNanos(50_000_000L);
                    continue;
                }

                // Choose direction and next stop.
                decideServingDirectionLocked();
                moveTowardsNextStopLocked();
            } catch (InterruptedException ie) {
                // allow graceful shutdown
            } finally {
                lock.unlock();
            }
        }
    }

    private void ensureStopAtFloorLocked(int floor) {
        // For emergency: always stop at floor 1.
        upStops.clear();
        downStops.clear();
        if (floor >= currentFloor) {
            upStops.add(floor);
        } else {
            downStops.add(floor);
        }
    }

    private void updateWeightExceededLocked() {
        boolean exceededNow = loadKg > weightLimitKg;
        if (exceededNow) {
            if (!weightExceededAnnounced) {
                weightExceededAnnounced = true;
                soundPlayer.play("WEIGHT_LIMIT_EXCEEDED");
                announcementService.announce(AnnouncementKey.WEIGHT_LIMIT_EXCEEDED, java.util.Locale.ENGLISH, null);
            }
            weightExceeded = true;
        } else {
            weightExceeded = false;
            weightExceededAnnounced = false;
            // When weight returns to normal, we can proceed; keep doorState as-is
            // (worker will auto-close after next stop).
        }
        // If weight changed, notify potentially waiting movement loop.
        weightChanged.signalAll();
    }

    private void decideServingDirectionLocked() {
        boolean hasUp = !upStops.isEmpty();
        boolean hasDown = !downStops.isEmpty();
        if (!hasUp && hasDown) {
            servingDirection = Direction.DOWN;
        } else if (hasUp && !hasDown) {
            servingDirection = Direction.UP;
        } else {
            // If both exist, keep current direction if it still has stops; otherwise switch.
            if (servingDirection == Direction.UP && hasUp) {
                // keep
            } else if (servingDirection == Direction.DOWN && hasDown) {
                // keep
            } else {
                servingDirection = hasUp ? Direction.UP : Direction.DOWN;
            }
        }
        if (servingDirection == Direction.UP) {
            state = ElevatorState.MOVING_UP;
        } else {
            state = ElevatorState.MOVING_DOWN;
        }
    }

    private void moveTowardsNextStopLocked() throws InterruptedException {
        // One "tick": move one floor towards the chosen next stop, then stop if we arrived.
        Integer next = peekNextStopLocked();
        if (next == null) return;

        if (doorState == DoorState.OPEN) {
            // Close doors after being open (if weight is normal and not emergency).
            doorState = DoorState.CLOSED;
        }

        if (currentFloor < next) currentFloor++;
        else if (currentFloor > next) currentFloor--;

        // If we reached a stop floor for the serving direction, stop and open doors.
        if (servingDirection == Direction.UP && upStops.contains(currentFloor)) {
            upStops.remove(currentFloor);
            stopAndArriveLocked(currentFloor, Direction.UP);
        } else if (servingDirection == Direction.DOWN && downStops.contains(currentFloor)) {
            downStops.remove(currentFloor);
            stopAndArriveLocked(currentFloor, Direction.DOWN);
        } else {
            // Continue moving; signal others if needed.
            Thread.sleep(10);
        }

        // If we reached emergency floor or last stop, briefly let door open and then close.
        if (doorState == DoorState.OPEN) {
            Thread.sleep(10);
            // Weight already checked to be ok; auto-close.
            doorState = DoorState.CLOSED;
        }
    }

    private Integer peekNextStopLocked() {
        if (servingDirection == Direction.UP) {
            NavigableSet<Integer> up = upStops;
            if (up.isEmpty()) return null;
            Integer first = up.first();
            return first;
        } else {
            if (downStops.isEmpty()) return null;
            return downStops.last();
        }
    }

    private void stopAndArriveLocked(int floor, Direction dir) {
        doorState = DoorState.OPEN;
        state = emergencyMode ? ElevatorState.EMERGENCY : ElevatorState.IDLE;
        idleSinceMs = System.currentTimeMillis();
        listener.onElevatorArrived(elevatorId, floor, dir);
    }

    void enqueueStop(int floor, Direction direction) {
        lock.lock();
        try {
            if (emergencyMode || maintenance) return;
            if (direction == Direction.UP) upStops.add(floor);
            else downStops.add(floor);
            // Wake up worker to process stop
            idleOrStopChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void pressDoorOpen() {
        lock.lock();
        try {
            if (emergencyMode) return;
            doorState = DoorState.OPEN;
            idleOrStopChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void pressDoorClose() {
        lock.lock();
        try {
            if (emergencyMode) return;
            if (weightExceeded) {
                // Keep doors open if weight exceeded.
                doorState = DoorState.OPEN;
            } else {
                doorState = DoorState.CLOSED;
            }
            idleOrStopChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
        lock.lock();
        try {
            idleOrStopChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void enterEmergencyMode() {
        emergencyMode = true;
        lock.lock();
        try {
            ensureStopAtFloorLocked(1);
            idleOrStopChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void clearEmergencyMode() {
        emergencyMode = false;
        lock.lock();
        try {
            idleOrStopChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void setLoadKg(double loadKg) {
        lock.lock();
        try {
            if (loadKg < 0) throw new IllegalArgumentException("loadKg cannot be negative");
            this.loadKg = loadKg;
            weightChanged.signalAll();
            idleOrStopChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    ElevatorSnapshot snapshot() {
        lock.lock();
        try {
            return new ElevatorSnapshot(elevatorId, currentFloor, state, doorState, weightExceeded);
        } finally {
            lock.unlock();
        }
    }

    long idleSinceMs() {
        lock.lock();
        try {
            return idleSinceMs;
        } finally {
            lock.unlock();
        }
    }

    int currentFloor() {
        lock.lock();
        try {
            return currentFloor;
        } finally {
            lock.unlock();
        }
    }

    String elevatorId() {
        return elevatorId;
    }

    boolean isOperationalCandidate() {
        return !maintenance && !emergencyMode;
    }

    boolean isExpressEnabled() {
        return expressEnabled;
    }

    void shutdown() {
        running = false;
        lock.lock();
        try {
            idleOrStopChanged.signalAll();
            weightChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }
}

interface ElevatorArrivalListener {
    void onElevatorArrived(String elevatorId, int floor, Direction directionServed);
}

