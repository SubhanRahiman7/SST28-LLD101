package com.example.elevator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ElevatorSystem facade + APIs.
 *
 * This is an in-memory LLD with a lightweight simulation of elevator movement/state.
 * The key focus is concurrency-safe assignment and a pluggable selection policy.
 */
public final class ElevatorSystem {
    private final ReentrantLock assignmentLock = new ReentrantLock(true);

    private final Map<String, ElevatorCar> carsById = new ConcurrentHashMap<>();
    private volatile int maxFloor = 0;

    // Disabled outside buttons: disabledDirectionsByFloor[floor] contains direction(s) blocked at that floor.
    private final Set<Integer> disabledFloors = ConcurrentHashMap.newKeySet();
    private final Map<Integer, EnumSet<Direction>> disabledDirectionsByFloor = new ConcurrentHashMap<>();

    // Outside call state.
    private final Map<String, CallRequest> pendingRequestsById = new ConcurrentHashMap<>();
    private final Map<String, String> assignedElevatorByRequestId = new ConcurrentHashMap<>();
    private final Set<String> completedRequests = ConcurrentHashMap.newKeySet();

    private volatile boolean emergencyMode = false;
    private volatile SelectionPolicy selectionPolicy;

    private final SoundPlayer soundPlayer;
    private final AnnouncementService announcementService;
    private volatile Locale announcementsLocale = Locale.ENGLISH;

    public ElevatorSystem(int initialMaxFloor, SoundPlayer soundPlayer, AnnouncementService announcementService) {
        if (initialMaxFloor <= 0) throw new IllegalArgumentException("initialMaxFloor must be > 0");
        this.maxFloor = initialMaxFloor;
        this.soundPlayer = Objects.requireNonNull(soundPlayer, "soundPlayer");
        this.announcementService = Objects.requireNonNull(announcementService, "announcementService");
        this.selectionPolicy = new ShortestSeekTimePolicy();
    }

    // -------------------------
    // Admin / configuration
    // -------------------------
    public void addFloors(int newMaxFloor) {
        if (newMaxFloor <= 0) throw new IllegalArgumentException("newMaxFloor must be > 0");
        if (newMaxFloor <= maxFloor) return;
        this.maxFloor = newMaxFloor;
    }

    public void addElevatorCar(String elevatorId, int weightLimitKg, boolean expressEnabled) {
        ElevatorCar existing = carsById.get(elevatorId);
        if (existing != null) throw new IllegalArgumentException("Elevator already exists: " + elevatorId);

        ElevatorCar car = new ElevatorCar(
                elevatorId,
                weightLimitKg,
                expressEnabled,
                1,
                new ArrivalListenerImpl(),
                soundPlayer,
                announcementService
        );
        if (carsById.putIfAbsent(elevatorId, car) != null) {
            throw new IllegalArgumentException("Elevator already exists: " + elevatorId);
        }

        Thread t = new Thread(car, "Elevator-" + elevatorId);
        t.setDaemon(true);
        t.start();
    }

    public void setSelectionPolicy(SelectionPolicy selectionPolicy) {
        this.selectionPolicy = Objects.requireNonNull(selectionPolicy, "selectionPolicy");
    }

    public void setAnnouncementsLocale(Locale locale) {
        this.announcementsLocale = locale == null ? Locale.ENGLISH : locale;
    }

    public void setMaintenance(String elevatorId, boolean underMaintenance) {
        ElevatorCar car = carsById.get(elevatorId);
        if (car == null) throw new IllegalArgumentException("Unknown elevatorId: " + elevatorId);
        car.setMaintenance(underMaintenance);
    }

    public void setLoadKg(String elevatorId, double loadKg) {
        ElevatorCar car = carsById.get(elevatorId);
        if (car == null) throw new IllegalArgumentException("Unknown elevatorId: " + elevatorId);
        car.setLoadKg(loadKg);
    }

    public void disableFloor(int floor) {
        validateFloor(floor);
        disabledFloors.add(floor);
    }

    public void enableFloor(int floor) {
        validateFloor(floor);
        disabledFloors.remove(floor);
    }

    public void disableOutsideButton(int floor, Direction direction) {
        validateFloor(floor);
        Objects.requireNonNull(direction, "direction");
        disabledDirectionsByFloor.compute(floor, (k, v) -> {
            EnumSet<Direction> set = (v == null) ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(v);
            set.add(direction);
            return set;
        });
    }

    public void enableOutsideButton(int floor, Direction direction) {
        validateFloor(floor);
        Objects.requireNonNull(direction, "direction");
        disabledDirectionsByFloor.computeIfPresent(floor, (k, v) -> {
            EnumSet<Direction> set = EnumSet.copyOf(v);
            set.remove(direction);
            return set.isEmpty() ? null : set;
        });
    }

    // Emergency: power outage / emergency alarm functionality.
    public void triggerEmergency() {
        emergencyMode = true;
        announcementService.announce(AnnouncementKey.EMERGENCY_MODE_ENABLED, announcementsLocale, null);

        // Clear pending requests; they won't be served in emergency.
        pendingRequestsById.clear();
        assignedElevatorByRequestId.clear();
        completedRequests.clear();

        for (ElevatorCar car : carsById.values()) {
            car.enterEmergencyMode();
        }
    }

    public void clearEmergency() {
        emergencyMode = false;
        for (ElevatorCar car : carsById.values()) {
            car.clearEmergencyMode();
        }
    }

    // -------------------------
    // User operations (buttons / door controls)
    // -------------------------
    public String pressOutsideButton(int floor, Direction direction, int priority, boolean expressPreferred) {
        validateFloor(floor);
        Objects.requireNonNull(direction, "direction");
        if (emergencyMode) {
            // In emergency mode we ignore new calls.
            return "REJECTED-EMERGENCY";
        }
        if (disabledFloors.contains(floor)) {
            return "REJECTED-DISABLED";
        }
        EnumSet<Direction> disabledDirs = disabledDirectionsByFloor.get(floor);
        if (disabledDirs != null && disabledDirs.contains(direction)) {
            return "REJECTED-DISABLED";
        }

        String requestId = "CALL-" + System.nanoTime();
        CallRequest request = new CallRequest(requestId, floor, direction, System.currentTimeMillis(), priority, expressPreferred);
        pendingRequestsById.put(requestId, request);

        assignSingleElevatorForRequest(request);
        return requestId;
    }

    public void pressInsideButton(String elevatorId, int floor) {
        validateFloor(floor);
        ElevatorCar car = carsById.get(elevatorId);
        if (car == null) throw new IllegalArgumentException("Unknown elevatorId: " + elevatorId);
        if (!car.isOperationalCandidate() || emergencyMode) return;

        Direction dir = (floor >= car.currentFloor()) ? Direction.UP : Direction.DOWN;
        car.enqueueStop(floor, dir);
    }

    public void requestDoorOpen(String elevatorId) {
        ElevatorCar car = carsById.get(elevatorId);
        if (car == null) throw new IllegalArgumentException("Unknown elevatorId: " + elevatorId);
        car.pressDoorOpen();
    }

    public void requestDoorClose(String elevatorId) {
        ElevatorCar car = carsById.get(elevatorId);
        if (car == null) throw new IllegalArgumentException("Unknown elevatorId: " + elevatorId);
        car.pressDoorClose();
    }

    // -------------------------
    // Queries / snapshots
    // -------------------------
    public ElevatorSnapshot getElevatorSnapshot(String elevatorId) {
        ElevatorCar car = carsById.get(elevatorId);
        if (car == null) throw new IllegalArgumentException("Unknown elevatorId: " + elevatorId);
        return car.snapshot();
    }

    public List<ElevatorSnapshot> getAllElevatorSnapshots() {
        List<ElevatorSnapshot> res = new ArrayList<>();
        for (ElevatorCar car : carsById.values()) {
            res.add(car.snapshot());
        }
        return res;
    }

    public boolean isRequestCompleted(String requestId) {
        return completedRequests.contains(requestId);
    }

    // -------------------------
    // Assignment
    // -------------------------
    private void assignSingleElevatorForRequest(CallRequest request) {
        assignmentLock.lock();
        try {
            // Ensure energy-efficient behavior: each call request gets exactly one elevator.
            assignedElevatorByRequestId.putIfAbsent(request.requestId, "ASSIGNING");

            List<ElevatorCar> candidates = new ArrayList<>();
            for (ElevatorCar car : carsById.values()) {
                if (car.isOperationalCandidate()) {
                    candidates.add(car);
                }
            }
            if (candidates.isEmpty()) {
                pendingRequestsById.remove(request.requestId);
                assignedElevatorByRequestId.remove(request.requestId);
                return;
            }

            String chosenId = selectionPolicy.selectElevator(request, candidates);
            ElevatorCar chosen = carsById.get(chosenId);
            if (chosen == null || !chosen.isOperationalCandidate()) {
                throw new IllegalStateException("Selection policy returned non-operational elevator: " + chosenId);
            }

            assignedElevatorByRequestId.put(request.requestId, chosenId);
            chosen.enqueueStop(request.floor, request.direction);
        } finally {
            assignmentLock.unlock();
        }
    }

    private void validateFloor(int floor) {
        if (floor < 1 || floor > maxFloor) {
            throw new IllegalArgumentException("floor out of range: " + floor + " (maxFloor=" + maxFloor + ")");
        }
    }

    // Listener that the ElevatorCar calls when it arrives at a floor.
    private final class ArrivalListenerImpl implements ElevatorArrivalListener {
        @Override
        public void onElevatorArrived(String elevatorId, int floor, Direction directionServed) {
            // Mark all matching pending requests completed (supports UP+DOWN simultaneously for same floor).
            for (Map.Entry<String, CallRequest> e : pendingRequestsById.entrySet()) {
                String requestId = e.getKey();
                CallRequest req = e.getValue();
                if (req.floor != floor) continue;
                if (req.direction != directionServed) continue;

                String assignedElevator = assignedElevatorByRequestId.get(requestId);
                if (!Objects.equals(assignedElevator, elevatorId)) continue;

                pendingRequestsById.remove(requestId);
                assignedElevatorByRequestId.remove(requestId);
                completedRequests.add(requestId);
            }
        }
    }

    public void shutdown() {
        for (ElevatorCar car : carsById.values()) {
            car.shutdown();
        }
    }
}

