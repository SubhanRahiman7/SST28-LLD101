package com.example.bookmyshow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Domain objects for the LLD: catalog (City/Theater/Screen/Show) and show seat inventory.
 *
 * Note: Most fields are intentionally simple (in-memory LLD) to keep this runnable with `javac`.
 */
enum SeatCategory {
    SILVER, GOLD, PLATINUM
}

enum SeatStatus {
    AVAILABLE, LOCKED, BOOKED
}

enum BookingStatus {
    CONFIRMED, CANCELLED
}

final class Movie {
    final String movieId;
    final String title;
    final String genre;
    final double showTierMultiplier; // Admin-configured "show-based" multiplier for this movie.

    Movie(String movieId, String title, String genre, double showTierMultiplier) {
        this.movieId = requireNonBlank(movieId, "movieId");
        this.title = requireNonBlank(title, "title");
        this.genre = requireNonBlank(genre, "genre");
        if (showTierMultiplier <= 0) throw new IllegalArgumentException("showTierMultiplier must be > 0");
        this.showTierMultiplier = showTierMultiplier;
    }

    static String requireNonBlank(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " cannot be null/blank");
        return v;
    }
}

final class City {
    final String cityId;
    final String name;
    final Map<String, Theater> theatersById = new ConcurrentHashMap<>();

    City(String cityId, String name) {
        this.cityId = Movie.requireNonBlank(cityId, "cityId");
        this.name = Movie.requireNonBlank(name, "name");
    }
}

final class Theater {
    final String theaterId;
    final String cityId;
    final String name;
    final Map<String, Screen> screensById = new ConcurrentHashMap<>();

    Theater(String theaterId, String cityId, String name) {
        this.theaterId = Movie.requireNonBlank(theaterId, "theaterId");
        this.cityId = Movie.requireNonBlank(cityId, "cityId");
        this.name = Movie.requireNonBlank(name, "name");
    }
}

final class Screen {
    final String screenId;
    final String theaterId;
    final int rows;
    final int cols;
    final Map<String, Show> showsById = new ConcurrentHashMap<>();

    Screen(String screenId, String theaterId, int rows, int cols) {
        this.screenId = Movie.requireNonBlank(screenId, "screenId");
        this.theaterId = Movie.requireNonBlank(theaterId, "theaterId");
        if (rows <= 0 || cols <= 0) throw new IllegalArgumentException("rows/cols must be > 0");
        this.rows = rows;
        this.cols = cols;
    }
}

final class SeatSnapshot {
    final String seatId;
    final SeatCategory category;
    final SeatStatus effectiveStatus;

    SeatSnapshot(String seatId, SeatCategory category, SeatStatus effectiveStatus) {
        this.seatId = seatId;
        this.category = category;
        this.effectiveStatus = effectiveStatus;
    }
}

final class ReservationToken {
    final String showId;
    final String reservationId;
    final String userId;
    final List<String> seatIds; // sorted/normalized
    final long lockedUntilMs;

    ReservationToken(String showId, String reservationId, String userId, List<String> seatIds, long lockedUntilMs) {
        this.showId = Movie.requireNonBlank(showId, "showId");
        this.reservationId = Movie.requireNonBlank(reservationId, "reservationId");
        this.userId = Movie.requireNonBlank(userId, "userId");
        this.seatIds = Collections.unmodifiableList(new ArrayList<>(seatIds));
        this.lockedUntilMs = lockedUntilMs;
    }

    boolean isExpired(long nowMs) {
        return nowMs > lockedUntilMs;
    }
}

final class Booking {
    final String bookingId;
    final String userId;
    final String showId;
    final List<String> seatIds; // sorted/normalized
    final double totalAmount;
    final String paymentId;
    final long createdAtMs;
    volatile BookingStatus status;

    Booking(
            String bookingId,
            String userId,
            String showId,
            List<String> seatIds,
            double totalAmount,
            String paymentId,
            long createdAtMs,
            BookingStatus status
    ) {
        this.bookingId = Movie.requireNonBlank(bookingId, "bookingId");
        this.userId = Movie.requireNonBlank(userId, "userId");
        this.showId = Movie.requireNonBlank(showId, "showId");
        this.seatIds = Collections.unmodifiableList(new ArrayList<>(seatIds));
        this.totalAmount = totalAmount;
        this.paymentId = Movie.requireNonBlank(paymentId, "paymentId");
        this.createdAtMs = createdAtMs;
        this.status = status;
    }
}

/**
 * Seat with per-seat lock (pessimistic locking) to prevent concurrent double-locking/double-booking.
 */
final class Seat {
    private final ReentrantLock seatLock = new ReentrantLock();

    final String seatId;
    final int rowIndex;
    final int colIndex;
    final SeatCategory category;

    private volatile SeatStatus status = SeatStatus.AVAILABLE;
    private volatile long lockedUntilMs = 0;
    private volatile String lockedReservationId = null;
    private volatile String bookedBookingId = null;

    Seat(String seatId, int rowIndex, int colIndex, SeatCategory category) {
        this.seatId = Movie.requireNonBlank(seatId, "seatId");
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.category = Objects.requireNonNull(category, "category");
    }

    void lock() {
        seatLock.lock();
    }

    void unlock() {
        seatLock.unlock();
    }

    SeatSnapshot snapshot(long nowMs) {
        SeatStatus effective = effectiveStatus(nowMs);
        return new SeatSnapshot(seatId, category, effective);
    }

    SeatStatus effectiveStatus(long nowMs) {
        SeatStatus s = status;
        if (s == SeatStatus.LOCKED && lockedUntilMs <= nowMs) {
            // Lazy expiry: treat as available without mutating state.
            return SeatStatus.AVAILABLE;
        }
        return s;
    }

    boolean isAvailableForLock(long nowMs) {
        return effectiveStatus(nowMs) == SeatStatus.AVAILABLE;
    }

    boolean isLockedByReservation(String reservationId) {
        return status == SeatStatus.LOCKED && reservationId.equals(lockedReservationId);
    }

    long lockedUntilMs() {
        return lockedUntilMs;
    }

    void lockForReservation(String reservationId, long lockedUntilMs) {
        this.status = SeatStatus.LOCKED;
        this.lockedReservationId = reservationId;
        this.lockedUntilMs = lockedUntilMs;
        this.bookedBookingId = null;
    }

    void releaseReservation(String reservationId) {
        if (status == SeatStatus.LOCKED && reservationId.equals(lockedReservationId)) {
            status = SeatStatus.AVAILABLE;
            lockedReservationId = null;
            lockedUntilMs = 0;
        }
    }

    void book(String bookingId) {
        this.status = SeatStatus.BOOKED;
        this.bookedBookingId = bookingId;
        this.lockedReservationId = null;
        this.lockedUntilMs = 0;
    }

    boolean isBookedForBooking(String bookingId) {
        return status == SeatStatus.BOOKED && bookingId.equals(bookedBookingId);
    }

    void cancelBooking(String bookingId) {
        if (isBookedForBooking(bookingId)) {
            status = SeatStatus.AVAILABLE;
            bookedBookingId = null;
        }
    }
}

/**
 * Show = fixed seat layout + availability + reservation/booking transitions.
 */
final class Show {
    final String showId;
    final String movieId;
    final String screenId;
    final LocalDateTime startTime;

    private final Map<String, Seat> seatsById = new ConcurrentHashMap<>();
    private final List<Seat> seatsInRowMajorOrder;

    Show(String showId, String movieId, String screenId, LocalDateTime startTime, int rows, int cols, int platinumRows, int goldRows) {
        this.showId = Movie.requireNonBlank(showId, "showId");
        this.movieId = Movie.requireNonBlank(movieId, "movieId");
        this.screenId = Movie.requireNonBlank(screenId, "screenId");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        if (rows <= 0 || cols <= 0) throw new IllegalArgumentException("rows/cols must be > 0");
        if (platinumRows < 0 || goldRows < 0) throw new IllegalArgumentException("platinumRows/goldRows must be >= 0");
        if (platinumRows + goldRows > rows) throw new IllegalArgumentException("platinumRows + goldRows must be <= rows");

        List<Seat> tmp = new ArrayList<>(rows * cols);
        int silverStart = platinumRows + goldRows;

        for (int r = 0; r < rows; r++) {
            SeatCategory cat;
            if (r < platinumRows) cat = SeatCategory.PLATINUM;
            else if (r < silverStart) cat = SeatCategory.GOLD;
            else cat = SeatCategory.SILVER;

            for (int c = 0; c < cols; c++) {
                String seatId = seatId(r, c);
                Seat seat = new Seat(seatId, r, c, cat);
                tmp.add(seat);
                seatsById.put(seatId, seat);
            }
        }
        this.seatsInRowMajorOrder = Collections.unmodifiableList(tmp);
    }

    static String seatId(int rowIndex, int colIndex) {
        return "R" + (rowIndex + 1) + "C" + (colIndex + 1);
    }

    Seat seatById(String seatId) {
        return seatsById.get(seatId);
    }

    List<SeatSnapshot> getSeatMap(long nowMs) {
        List<SeatSnapshot> res = new ArrayList<>(seatsInRowMajorOrder.size());
        for (Seat s : seatsInRowMajorOrder) {
            res.add(s.snapshot(nowMs));
        }
        return res;
    }

    int totalSeats() {
        return seatsInRowMajorOrder.size();
    }

    int bookedSeatsCount() {
        int count = 0;
        for (Seat seat : seatsInRowMajorOrder) {
            if (seat.effectiveStatus(System.currentTimeMillis()) == SeatStatus.BOOKED) count++;
        }
        return count;
    }

    ReservationToken lockSeats(List<String> requestedSeatIds, String userId, long nowMs, long seatLockDurationMs) {
        if (requestedSeatIds == null || requestedSeatIds.isEmpty()) {
            throw new IllegalArgumentException("requestedSeatIds cannot be null/empty");
        }

        List<String> seatIds = normalizeAndSortSeatIds(requestedSeatIds);
        List<Seat> toLock = new ArrayList<>(seatIds.size());
        for (String seatId : seatIds) {
            Seat seat = seatsById.get(seatId);
            if (seat == null) throw new IllegalArgumentException("Unknown seat: " + seatId);
            toLock.add(seat);
        }

        // Pessimistic locking: acquire all seat locks in a deterministic order.
        for (Seat seat : toLock) seat.lock();
        try {
            for (Seat seat : toLock) {
                if (!seat.isAvailableForLock(nowMs)) {
                    throw new IllegalStateException("Seat is not available: " + seat.seatId + " (effective=" + seat.effectiveStatus(nowMs) + ")");
                }
            }

            String reservationId = "RES-" + UUID.randomUUID();
            long lockedUntilMs = nowMs + seatLockDurationMs;
            for (Seat seat : toLock) {
                seat.lockForReservation(reservationId, lockedUntilMs);
            }
            return new ReservationToken(showId, reservationId, userId, seatIds, lockedUntilMs);
        } finally {
            for (Seat seat : toLock) seat.unlock();
        }
    }

    void releaseReservation(ReservationToken token) {
        Objects.requireNonNull(token, "token");
        List<String> seatIds = token.seatIds;
        List<Seat> toRelease = new ArrayList<>(seatIds.size());
        for (String seatId : seatIds) {
            Seat seat = seatsById.get(seatId);
            if (seat != null) toRelease.add(seat);
        }

        // Acquire locks to make state updates consistent.
        for (Seat seat : toRelease) seat.lock();
        try {
            for (Seat seat : toRelease) {
                seat.releaseReservation(token.reservationId);
            }
        } finally {
            for (Seat seat : toRelease) seat.unlock();
        }
    }

    Booking confirmBooking(ReservationToken token, String paymentId, double totalAmount, long nowMs) {
        Objects.requireNonNull(token, "token");
        if (!showId.equals(token.showId)) throw new IllegalArgumentException("Token showId mismatch");
        if (token.isExpired(nowMs)) {
            // Reservation expired: treat it as failed and release.
            releaseReservation(token);
            throw new IllegalStateException("Reservation expired; seats were released.");
        }

        List<String> seatIds = token.seatIds;
        List<Seat> toBook = new ArrayList<>(seatIds.size());
        for (String seatId : seatIds) {
            Seat seat = seatsById.get(seatId);
            if (seat == null) throw new IllegalStateException("Seat disappeared: " + seatId);
            toBook.add(seat);
        }

        for (Seat seat : toBook) seat.lock();
        try {
            // Validate all seats before mutating (atomic "all-or-none").
            for (Seat seat : toBook) {
                if (!seat.isLockedByReservation(token.reservationId)) {
                    throw new IllegalStateException("Seat state changed; cannot confirm booking: " + seat.seatId);
                }
                if (seat.lockedUntilMs() <= nowMs) {
                    throw new IllegalStateException("Seat reservation expired; cannot confirm booking: " + seat.seatId);
                }
            }

            String bookingId = "BKG-" + UUID.randomUUID();
            for (Seat seat : toBook) {
                seat.book(bookingId);
            }
            return new Booking(
                    bookingId,
                    token.userId,
                    showId,
                    seatIds,
                    totalAmount,
                    paymentId,
                    nowMs,
                    BookingStatus.CONFIRMED
            );
        } finally {
            for (Seat seat : toBook) seat.unlock();
        }
    }

    void cancelBooking(Booking booking) {
        Objects.requireNonNull(booking, "booking");
        if (!showId.equals(booking.showId)) throw new IllegalArgumentException("Booking showId mismatch");
        List<String> seatIds = booking.seatIds;

        List<Seat> toCancel = new ArrayList<>(seatIds.size());
        for (String seatId : seatIds) {
            Seat seat = seatsById.get(seatId);
            if (seat == null) throw new IllegalStateException("Seat disappeared: " + seatId);
            toCancel.add(seat);
        }

        for (Seat seat : toCancel) seat.lock();
        try {
            for (Seat seat : toCancel) {
                if (!seat.isBookedForBooking(booking.bookingId)) {
                    throw new IllegalStateException("Seat is not booked for this booking: " + seat.seatId);
                }
            }
            for (Seat seat : toCancel) {
                seat.cancelBooking(booking.bookingId);
            }
        } finally {
            for (Seat seat : toCancel) seat.unlock();
        }
    }

    private static List<String> normalizeAndSortSeatIds(List<String> seatIds) {
        List<String> out = new ArrayList<>(seatIds.size());
        for (String id : seatIds) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("seatId cannot be null/blank");
            out.add(id.trim());
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }
}

