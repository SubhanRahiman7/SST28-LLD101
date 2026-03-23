package com.example.parkinglot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ParkingLot {
    private static final long MILLIS_PER_HOUR = 60L * 60L * 1000L;

    private final List<ParkingLevel> levels;
    private final Map<SlotType, Long> hourlyRates;
    private final Map<Integer, ParkingSlot> slotByNumber;
    private final Map<Integer, ParkingTicket> activeTickets;
    private int ticketCounter;

    public ParkingLot(List<ParkingLevel> levels, Map<SlotType, Long> hourlyRates) {
        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException("levels cannot be null/empty");
        }
        if (hourlyRates == null) {
            throw new IllegalArgumentException("hourlyRates cannot be null");
        }
        for (SlotType t : SlotType.values()) {
            if (!hourlyRates.containsKey(t) || hourlyRates.get(t) == null || hourlyRates.get(t) < 0) {
                throw new IllegalArgumentException("hourlyRates must contain non-negative rates for all slot types");
            }
        }

        this.levels = new ArrayList<>(levels);
        this.hourlyRates = new HashMap<>(hourlyRates);
        this.slotByNumber = new HashMap<>();
        this.activeTickets = new HashMap<>();
        this.ticketCounter = 0;

        for (ParkingLevel level : this.levels) {
            for (ParkingSlot slot : level.getSlots()) {
                if (slotByNumber.containsKey(slot.getSlotNumber())) {
                    throw new IllegalArgumentException("Duplicate slot number: " + slot.getSlotNumber());
                }
                slotByNumber.put(slot.getSlotNumber(), slot);
            }
        }
    }

    // park(vehicleDetails, entryTime, requestedSlotType, entryGateID)
    public ParkingTicket park(String vehicleDetails, long entryTimeMillis, SlotType requestedSlotType, int entryGateID) {
        if (vehicleDetails == null || vehicleDetails.isBlank()) {
            throw new IllegalArgumentException("vehicleDetails cannot be blank");
        }
        if (entryTimeMillis < 0) {
            throw new IllegalArgumentException("entryTimeMillis cannot be negative");
        }
        if (requestedSlotType == null) {
            throw new IllegalArgumentException("requestedSlotType cannot be null");
        }
        if (entryGateID < 0) {
            throw new IllegalArgumentException("entryGateID cannot be negative");
        }

        List<SlotType> compatibleTypes = compatibleSlotTypes(requestedSlotType);
        int gateNearestLevel = entryGateID % levels.size();

        ParkingSlot best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (ParkingLevel level : levels) {
            int distance = Math.abs(level.getLevelIndex() - gateNearestLevel);
            for (ParkingSlot slot : level.getSlots()) {
                if (!slot.isAvailable()) {
                    continue;
                }
                if (!compatibleTypes.contains(slot.getSlotType())) {
                    continue;
                }

                int candidateSlotNumber = slot.getSlotNumber();
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = slot;
                } else if (distance == bestDistance && best != null && candidateSlotNumber < best.getSlotNumber()) {
                    best = slot;
                }
            }
        }

        if (best == null) {
            throw new IllegalStateException("No compatible slots available for requestedSlotType=" + requestedSlotType);
        }

        best.allocate();
        int ticketId = ++ticketCounter;
        ParkingTicket ticket = new ParkingTicket(
                ticketId,
                vehicleDetails,
                best.getSlotNumber(),
                best.getSlotType(),
                entryTimeMillis,
                entryGateID
        );
        activeTickets.put(ticketId, ticket);
        return ticket;
    }

    // status()
    public Map<SlotType, Integer> status() {
        Map<SlotType, Integer> availableCount = new HashMap<>();
        for (SlotType t : SlotType.values()) {
            availableCount.put(t, 0);
        }

        for (ParkingLevel level : levels) {
            for (ParkingSlot slot : level.getSlots()) {
                if (slot.isAvailable()) {
                    availableCount.put(slot.getSlotType(), availableCount.get(slot.getSlotType()) + 1);
                }
            }
        }
        return availableCount;
    }

    // exit(parkingTicket, exitTime)
    // returns total bill amount
    public long exit(ParkingTicket parkingTicket, long exitTimeMillis) {
        if (parkingTicket == null) {
            throw new IllegalArgumentException("parkingTicket cannot be null");
        }
        if (exitTimeMillis < 0) {
            throw new IllegalArgumentException("exitTimeMillis cannot be negative");
        }

        ParkingTicket active = activeTickets.get(parkingTicket.getTicketId());
        if (active == null) {
            throw new IllegalStateException("Invalid or already-closed parking ticket: " + parkingTicket.getTicketId());
        }
        if (exitTimeMillis < active.getEntryTimeMillis()) {
            throw new IllegalArgumentException("exitTimeMillis cannot be earlier than entryTimeMillis");
        }

        ParkingSlot slot = slotByNumber.get(active.getAllocatedSlotNumber());
        if (slot == null) {
            throw new IllegalStateException("Allocated slot not found: " + active.getAllocatedSlotNumber());
        }

        long durationMillis = exitTimeMillis - active.getEntryTimeMillis();
        long hours = (durationMillis + MILLIS_PER_HOUR - 1) / MILLIS_PER_HOUR; // ceil
        if (hours < 0) {
            hours = 0;
        }

        long rate = hourlyRates.get(active.getAllocatedSlotType());
        long bill = hours * rate;

        // Free resources.
        slot.free();
        activeTickets.remove(active.getTicketId());
        return bill;
    }

    private List<SlotType> compatibleSlotTypes(SlotType requestedSlotType) {
        // Compatibility rules:
        // - 2-wheeler (requested SMALL) => SMALL, MEDIUM, LARGE
        // - car (requested MEDIUM) => MEDIUM, LARGE
        // - bus (requested LARGE) => LARGE only
        List<SlotType> types = new ArrayList<>();
        switch (requestedSlotType) {
            case SMALL:
                types.add(SlotType.SMALL);
                types.add(SlotType.MEDIUM);
                types.add(SlotType.LARGE);
                return types;
            case MEDIUM:
                types.add(SlotType.MEDIUM);
                types.add(SlotType.LARGE);
                return types;
            case LARGE:
                types.add(SlotType.LARGE);
                return types;
            default:
                throw new IllegalArgumentException("Unknown requestedSlotType: " + requestedSlotType);
        }
    }
}

