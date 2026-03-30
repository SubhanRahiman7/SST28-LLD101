package com.example.elevator;

import java.util.Comparator;
import java.util.List;

interface SelectionPolicy {
    /**
     * Must choose exactly one elevator id for a given outside call request.
     * Candidates list includes only operational elevators (not in maintenance/emergency).
     */
    String selectElevator(CallRequest request, List<ElevatorCar> candidates);
}

final class FirstComeFirstServePolicy implements SelectionPolicy {
    @Override
    public String selectElevator(CallRequest request, List<ElevatorCar> candidates) {
        // FCFS for elevators: choose the elevator that has been idle the longest.
        return candidates.stream()
                .min(Comparator
                        .comparingLong(ElevatorCar::idleSinceMs)
                        .thenComparing(ElevatorCar::elevatorId))
                .map(ElevatorCar::elevatorId)
                .orElseThrow(() -> new IllegalStateException("No candidate elevators"));
    }
}

final class ShortestSeekTimePolicy implements SelectionPolicy {
    @Override
    public String selectElevator(CallRequest request, List<ElevatorCar> candidates) {
        // Shortest seek time: min distance between current floor and requested floor.
        return candidates.stream()
                .min(Comparator
                        .comparingInt((ElevatorCar e) -> Math.abs(e.currentFloor() - request.floor))
                        .thenComparing(ElevatorCar::elevatorId))
                .map(ElevatorCar::elevatorId)
                .orElseThrow(() -> new IllegalStateException("No candidate elevators"));
    }
}

final class ExpressPriorityPolicy implements SelectionPolicy {
    private final java.util.Set<Integer> expressFloors;

    ExpressPriorityPolicy(java.util.Set<Integer> expressFloors) {
        this.expressFloors = expressFloors == null ? java.util.Set.of() : expressFloors;
    }

    @Override
    public String selectElevator(CallRequest request, List<ElevatorCar> candidates) {
        boolean preferExpress = request.expressPreferred || expressFloors.contains(request.floor);

        // If express is not preferred, behave like shortest seek.
        if (!preferExpress) {
            return new ShortestSeekTimePolicy().selectElevator(request, candidates);
        }

        // Prefer express-enabled cars when express is preferred.
        List<ElevatorCar> expressPool = candidates.stream()
                .filter(ElevatorCar::isExpressEnabled)
                .toList();
        List<ElevatorCar> pool = expressPool.isEmpty() ? candidates : expressPool;
        return new ShortestSeekTimePolicy().selectElevator(request, pool);
    }
}

