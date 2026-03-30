package com.example.bookmyshow;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public final class App {
    public static void main(String[] args) {
        // Pricing configuration (admin).
        PricingEngine pricingEngine = new PricingEngine();
        pricingEngine.setBasePrice(SeatCategory.SILVER, 50);
        pricingEngine.setBasePrice(SeatCategory.GOLD, 80);
        pricingEngine.setBasePrice(SeatCategory.PLATINUM, 120);

        pricingEngine.addRule(new ShowBasedPricingRule(1.0)); // movie/show tier multiplier
        pricingEngine.addRule(new DayTimePricingRule(1.25, 18, 23, 1.10)); // peak + weekend
        pricingEngine.addRule(new DemandBasedSurgePricingRule(0.75)); // demand-based surge up to ~1.75x

        // Payment integration (mock).
        PaymentGatewayMock paymentGateway = new PaymentGatewayMock();

        // Seat lock duration (5 minutes by default; shortened here for the demo).
        BookingSystem bookingSystem = new BookingSystem(20_000L, paymentGateway, pricingEngine);

        // -------------------------
        // Admin setup
        // -------------------------
        bookingSystem.addOrUpdateCity("C1", "Mumbai");
        bookingSystem.addOrUpdateMovie("M1", "Interstellar", "SCI-FI", 1.3);
        bookingSystem.addOrUpdateMovie("M2", "Inception", "THRILLER", 1.2);

        bookingSystem.addOrUpdateTheater("C1", "T1", "PVR Cinemas");
        bookingSystem.addScreen("T1", "S1", 5, 8); // 5 rows, 8 columns

        bookingSystem.addShow("SH1", "S1", "M1", LocalDateTime.now().plusHours(3), 2, 2); // platinumRows=2, goldRows=2, rest silver

        // -------------------------
        // User flow: View -> Lock -> Pay -> Confirm
        // -------------------------
        System.out.println("=== Seat map before booking ===");
        printSeatStatus(bookingSystem.viewSeatMap("SH1"), Arrays.asList("R1C1", "R1C2", "R1C3"));

        ReservationToken token1 = bookingSystem.selectSeats("SH1", Arrays.asList("R1C1", "R1C2"), "U1");
        BookingResult result1 = bookingSystem.confirmBooking(token1, PaymentMethod.CARD);

        System.out.println("\nPayment state history (booking #1): " + result1.paymentStateHistory);
        if (result1.isSuccess()) {
            System.out.println("Booking confirmed: " + result1.booking.bookingId);
        } else {
            System.out.println("Booking failed.");
        }

        System.out.println("\n=== Seat map after booking ===");
        printSeatStatus(bookingSystem.viewSeatMap("SH1"), Arrays.asList("R1C1", "R1C2", "R1C3"));

        // -------------------------
        // Cancellation (before show time)
        // -------------------------
        System.out.println("\nCancelling booking: " + result1.booking.bookingId);
        Booking cancelled = bookingSystem.cancelBooking(result1.booking.bookingId);
        System.out.println("Booking status: " + cancelled.status);

        System.out.println("\n=== Seat map after cancellation ===");
        printSeatStatus(bookingSystem.viewSeatMap("SH1"), Arrays.asList("R1C1", "R1C2", "R1C3"));

        // -------------------------
        // User flow: Lock -> Payment FAIL -> Release seats
        // -------------------------
        paymentGateway.forceNextPaymentFailure();
        ReservationToken token2 = bookingSystem.selectSeats("SH1", List.of("R1C3"), "U2");
        BookingResult result2 = bookingSystem.confirmBooking(token2, PaymentMethod.UPI);

        System.out.println("\nPayment state history (booking #2): " + result2.paymentStateHistory);
        System.out.println("Booking present? " + (result2.booking != null));

        System.out.println("\n=== Seat map after failed payment (should be AVAILABLE) ===");
        printSeatStatus(bookingSystem.viewSeatMap("SH1"), List.of("R1C3"));
    }

    private static void printSeatStatus(List<SeatSnapshot> seats, List<String> seatIds) {
        for (String seatId : seatIds) {
            SeatSnapshot found = null;
            for (SeatSnapshot s : seats) {
                if (s.seatId.equals(seatId)) {
                    found = s;
                    break;
                }
            }
            if (found == null) {
                System.out.println(seatId + ": UNKNOWN");
            } else {
                System.out.println(seatId + " -> " + found.effectiveStatus + " (" + found.category + ")");
            }
        }
    }
}

