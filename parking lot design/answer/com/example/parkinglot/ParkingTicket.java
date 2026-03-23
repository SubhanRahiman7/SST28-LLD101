package com.example.parkinglot;

public final class ParkingTicket {
    private final int ticketId;
    private final String vehicleDetails;
    private final int allocatedSlotNumber;
    private final SlotType allocatedSlotType;
    private final long entryTimeMillis;
    private final int entryGateID;

    ParkingTicket(
            int ticketId,
            String vehicleDetails,
            int allocatedSlotNumber,
            SlotType allocatedSlotType,
            long entryTimeMillis,
            int entryGateID
    ) {
        if (ticketId <= 0) {
            throw new IllegalArgumentException("ticketId must be > 0");
        }
        if (vehicleDetails == null || vehicleDetails.isBlank()) {
            throw new IllegalArgumentException("vehicleDetails cannot be blank");
        }
        if (allocatedSlotNumber <= 0) {
            throw new IllegalArgumentException("allocatedSlotNumber must be > 0");
        }
        if (allocatedSlotType == null) {
            throw new IllegalArgumentException("allocatedSlotType cannot be null");
        }
        if (entryTimeMillis < 0) {
            throw new IllegalArgumentException("entryTimeMillis cannot be negative");
        }
        this.ticketId = ticketId;
        this.vehicleDetails = vehicleDetails;
        this.allocatedSlotNumber = allocatedSlotNumber;
        this.allocatedSlotType = allocatedSlotType;
        this.entryTimeMillis = entryTimeMillis;
        this.entryGateID = entryGateID;
    }

    public int getTicketId() {
        return ticketId;
    }

    public String getVehicleDetails() {
        return vehicleDetails;
    }

    public int getAllocatedSlotNumber() {
        return allocatedSlotNumber;
    }

    public SlotType getAllocatedSlotType() {
        return allocatedSlotType;
    }

    public long getEntryTimeMillis() {
        return entryTimeMillis;
    }

    public int getEntryGateID() {
        return entryGateID;
    }
}

