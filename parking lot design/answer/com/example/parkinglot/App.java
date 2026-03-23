package com.example.parkinglot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public final class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int levels = readPositiveInt(scanner, "Enter number of levels: ");
        int gates = readPositiveInt(scanner, "Enter number of entry gates: ");

        int smallPerLevel = readNonNegativeInt(scanner, "Enter # small slots per level: ");
        int mediumPerLevel = readNonNegativeInt(scanner, "Enter # medium slots per level: ");
        int largePerLevel = readNonNegativeInt(scanner, "Enter # large slots per level: ");

        long smallRate = readNonNegativeLong(scanner, "Enter hourly rate for SMALL slots: ");
        long mediumRate = readNonNegativeLong(scanner, "Enter hourly rate for MEDIUM slots: ");
        long largeRate = readNonNegativeLong(scanner, "Enter hourly rate for LARGE slots: ");

        Map<SlotType, Long> rates = new HashMap<>();
        rates.put(SlotType.SMALL, smallRate);
        rates.put(SlotType.MEDIUM, mediumRate);
        rates.put(SlotType.LARGE, largeRate);

        ParkingLot parkingLot = buildParkingLot(levels, smallPerLevel, mediumPerLevel, largePerLevel, rates);

        Map<Integer, ParkingTicket> localTickets = new HashMap<>();
        long baseTimeMillis = System.currentTimeMillis();

        while (true) {
            System.out.println();
            System.out.println("Choose option: 1=park, 2=status, 3=exit, 4=quit");
            String choice = scanner.nextLine().trim();
            if ("4".equals(choice)) {
                break;
            }

            if ("1".equals(choice)) {
                System.out.print("vehicleDetails (e.g., KA01AB1234): ");
                String vehicleDetails = scanner.nextLine().trim();

                SlotType requested = readSlotType(scanner, "requestedSlotType (small/medium/large): ");
                int entryGateID = readNonNegativeInt(scanner, "entryGateID (0 to " + (gates - 1) + "): ");
                if (entryGateID >= gates) {
                    System.out.println("entryGateID out of range; using " + (entryGateID % gates));
                    entryGateID = entryGateID % gates;
                }

                int entryMinute = readNonNegativeInt(scanner, "entryTime in minutes from base (e.g., 0): ");
                long entryTimeMillis = baseTimeMillis + entryMinute * 60L * 1000L;

                try {
                    ParkingTicket ticket = parkingLot.park(vehicleDetails, entryTimeMillis, requested, entryGateID);
                    localTickets.put(ticket.getTicketId(), ticket);
                    System.out.println("Parked! ticketId=" + ticket.getTicketId()
                            + ", slotNumber=" + ticket.getAllocatedSlotNumber()
                            + ", slotType=" + ticket.getAllocatedSlotType());
                } catch (RuntimeException ex) {
                    System.out.println("Could not park: " + ex.getMessage());
                }

            } else if ("2".equals(choice)) {
                Map<SlotType, Integer> availability = parkingLot.status();
                System.out.println("Available slots:");
                for (SlotType t : SlotType.values()) {
                    System.out.println("  " + t + ": " + availability.get(t));
                }

            } else if ("3".equals(choice)) {
                int ticketId = readPositiveInt(scanner, "parkingTicket.ticketId: ");
                ParkingTicket ticket = localTickets.get(ticketId);
                if (ticket == null) {
                    System.out.println("Unknown ticketId (not present in this demo session).");
                    continue;
                }

                int exitMinute = readNonNegativeInt(scanner, "exitTime in minutes from base: ");
                long exitTimeMillis = baseTimeMillis + exitMinute * 60L * 1000L;

                try {
                    long bill = parkingLot.exit(ticket, exitTimeMillis);
                    System.out.println("Exit successful! Bill amount = " + bill);
                    localTickets.remove(ticketId);
                } catch (RuntimeException ex) {
                    System.out.println("Could not exit: " + ex.getMessage());
                }

            } else {
                System.out.println("Invalid option. Try again.");
            }
        }

        System.out.println("Demo ended.");
    }

    private static ParkingLot buildParkingLot(
            int levels,
            int smallPerLevel,
            int mediumPerLevel,
            int largePerLevel,
            Map<SlotType, Long> rates
    ) {
        int slotNumber = 1;
        List<ParkingLevel> levelList = new ArrayList<>();
        for (int lvl = 0; lvl < levels; lvl++) {
            List<ParkingSlot> slots = new ArrayList<>();
            for (int i = 0; i < smallPerLevel; i++) {
                slots.add(new ParkingSlot(slotNumber++, SlotType.SMALL, lvl));
            }
            for (int i = 0; i < mediumPerLevel; i++) {
                slots.add(new ParkingSlot(slotNumber++, SlotType.MEDIUM, lvl));
            }
            for (int i = 0; i < largePerLevel; i++) {
                slots.add(new ParkingSlot(slotNumber++, SlotType.LARGE, lvl));
            }
            levelList.add(new ParkingLevel(lvl, slots));
        }
        return new ParkingLot(levelList, rates);
    }

    private static int readPositiveInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                int v = Integer.parseInt(raw);
                if (v > 0) {
                    return v;
                }
                System.out.println("Must be > 0.");
            } catch (NumberFormatException ex) {
                System.out.println("Invalid integer.");
            }
        }
    }

    private static int readNonNegativeInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                int v = Integer.parseInt(raw);
                if (v >= 0) {
                    return v;
                }
                System.out.println("Must be >= 0.");
            } catch (NumberFormatException ex) {
                System.out.println("Invalid integer.");
            }
        }
    }

    private static long readNonNegativeLong(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                long v = Long.parseLong(raw);
                if (v >= 0) {
                    return v;
                }
                System.out.println("Must be >= 0.");
            } catch (NumberFormatException ex) {
                System.out.println("Invalid number.");
            }
        }
    }

    private static SlotType readSlotType(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if ("small".equals(raw)) {
                return SlotType.SMALL;
            }
            if ("medium".equals(raw)) {
                return SlotType.MEDIUM;
            }
            if ("large".equals(raw)) {
                return SlotType.LARGE;
            }
            System.out.println("Enter one of: small, medium, large");
        }
    }
}

