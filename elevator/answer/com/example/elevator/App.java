package com.example.elevator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class App {
    public static void main(String[] args) throws Exception {
        SoundPlayer soundPlayer = new MockSoundPlayer();
        AnnouncementService announcementService = new ConsoleAnnouncementService();

        ElevatorSystem system = new ElevatorSystem(10, soundPlayer, announcementService);
        system.setAnnouncementsLocale(Locale.ENGLISH);

        // Express floors are optional; express elevators are preferred when expressPreferred=true.
        Set<Integer> expressFloors = Set.of(1, 5, 10);
        system.setSelectionPolicy(new ExpressPriorityPolicy(expressFloors));

        // Add elevators (A1 starts heavy to trigger weight-limit behavior).
        system.addElevatorCar("A1", 700, false);
        system.addElevatorCar("A2", 900, true);

        // Trigger weight limit exceed on A1.
        system.setLoadKg("A1", 900); // > 700 => weight exceeded
        system.setLoadKg("A2", 100);

        // Concurrent outside button presses (includes both UP and DOWN at same floor).
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Callable<String>> presses = new ArrayList<>();
        presses.add(() -> system.pressOutsideButton(3, Direction.UP, 1, false));   // should assign based on policy
        presses.add(() -> system.pressOutsideButton(7, Direction.DOWN, 1, false));
        presses.add(() -> system.pressOutsideButton(5, Direction.UP, 2, true));     // expressPreferred
        presses.add(() -> system.pressOutsideButton(5, Direction.DOWN, 2, false)); // simultaneous opposite direction at same floor

        List<Future<String>> results = pool.invokeAll(presses);
        pool.shutdown();

        for (Future<String> f : results) {
            System.out.println("RequestId: " + f.get());
        }

        // Give time for movement simulation.
        Thread.sleep(600);

        System.out.println("\n=== Snapshots (before weight cleared) ===");
        for (ElevatorSnapshot s : system.getAllElevatorSnapshots()) {
            printSnapshot(s);
        }

        // Clear weight exceeded so A1 can move and serve remaining stops.
        system.setLoadKg("A1", 300);

        Thread.sleep(900);
        System.out.println("\n=== Snapshots (after weight cleared) ===");
        for (ElevatorSnapshot s : system.getAllElevatorSnapshots()) {
            printSnapshot(s);
        }

        // Emergency example (power outage / emergency alarm).
        System.out.println("\n=== Triggering emergency ===");
        system.triggerEmergency();
        Thread.sleep(600);

        System.out.println("\n=== Snapshots (during emergency) ===");
        for (ElevatorSnapshot s : system.getAllElevatorSnapshots()) {
            printSnapshot(s);
        }

        system.shutdown();
    }

    private static void printSnapshot(ElevatorSnapshot s) {
        System.out.println(
                s.elevatorId +
                        " | floor=" + s.currentFloor +
                        " | state=" + s.state +
                        " | door=" + s.doorState +
                        " | weightExceeded=" + s.weightExceeded
        );
    }
}

