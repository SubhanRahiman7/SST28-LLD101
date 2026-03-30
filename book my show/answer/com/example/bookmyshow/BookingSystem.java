package com.example.bookmyshow;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facade that provides both Admin operations and User booking operations.
 * This is intentionally an in-memory LLD implementation to demonstrate:
 * - seat map + availability
 * - seat locking with pessimistic concurrency control
 * - atomic booking (all seats booked or none)
 * - payment (mock) with required payment states
 * - cancellation + refund trigger
 */
public final class BookingSystem {
    private static final long DEFAULT_SEAT_LOCK_DURATION_MS = 5 * 60 * 1000L;

    private final long seatLockDurationMs;
    private final PaymentGateway paymentGateway;
    private final PricingEngine pricingEngine;

    private final Map<String, Movie> moviesById = new ConcurrentHashMap<>();
    private final Map<String, City> citiesById = new ConcurrentHashMap<>();
    private final Map<String, Show> showsById = new ConcurrentHashMap<>();
    private final Map<String, Booking> bookingsById = new ConcurrentHashMap<>();

    public BookingSystem(PaymentGateway paymentGateway, PricingEngine pricingEngine) {
        this(DEFAULT_SEAT_LOCK_DURATION_MS, paymentGateway, pricingEngine);
    }

    public BookingSystem(long seatLockDurationMs, PaymentGateway paymentGateway, PricingEngine pricingEngine) {
        if (seatLockDurationMs <= 0) throw new IllegalArgumentException("seatLockDurationMs must be > 0");
        this.seatLockDurationMs = seatLockDurationMs;
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway");
        this.pricingEngine = Objects.requireNonNull(pricingEngine, "pricingEngine");
    }

    // -------------------------
    // Admin Operations (Catalog)
    // -------------------------
    public void addOrUpdateCity(String cityId, String cityName) {
        City city = new City(cityId, cityName);
        citiesById.put(city.cityId, city);
    }

    public void addOrUpdateMovie(String movieId, String title, String genre, double showTierMultiplier) {
        Movie movie = new Movie(movieId, title, genre, showTierMultiplier);
        moviesById.put(movie.movieId, movie);
    }

    public void addOrUpdateTheater(String cityId, String theaterId, String theaterName) {
        City city = citiesById.get(cityId);
        if (city == null) throw new IllegalArgumentException("Unknown cityId: " + cityId);
        Theater theater = new Theater(theaterId, cityId, theaterName);
        city.theatersById.put(theater.theaterId, theater);
    }

    public void addScreen(String theaterId, String screenId, int rows, int cols) {
        Theater theater = findTheaterById(theaterId);
        if (theater == null) throw new IllegalArgumentException("Unknown theaterId: " + theaterId);
        Screen screen = new Screen(screenId, theaterId, rows, cols);
        theater.screensById.put(screen.screenId, screen);
    }

    public void addShow(
            String showId,
            String screenId,
            String movieId,
            LocalDateTime startTime,
            int platinumRows,
            int goldRows
    ) {
        Screen screen = findScreenById(screenId);
        if (screen == null) throw new IllegalArgumentException("Unknown screenId: " + screenId);
        if (!moviesById.containsKey(movieId)) throw new IllegalArgumentException("Unknown movieId: " + movieId);
        Objects.requireNonNull(startTime, "startTime");

        // Each show has a fixed seat layout (row-based category distribution).
        Show show = new Show(showId, movieId, screenId, startTime, screen.rows, screen.cols, platinumRows, goldRows);
        screen.showsById.put(show.showId, show);
        showsById.put(show.showId, show);
    }

    public void setBasePrice(SeatCategory category, double basePrice) {
        pricingEngine.setBasePrice(category, basePrice);
    }

    public void addPricingRule(PricingRule rule) {
        pricingEngine.addRule(rule);
    }

    // -------------------------
    // User Operations (View/Book)
    // -------------------------
    public List<Movie> viewMoviesInCity(String cityId) {
        City city = citiesById.get(cityId);
        if (city == null) throw new IllegalArgumentException("Unknown cityId: " + cityId);

        Set<String> movieIds = new HashSet<>();
        for (Theater theater : city.theatersById.values()) {
            for (Screen screen : theater.screensById.values()) {
                for (Show show : screen.showsById.values()) {
                    movieIds.add(show.movieId);
                }
            }
        }

        List<Movie> movies = new ArrayList<>();
        for (String mid : movieIds) {
            Movie m = moviesById.get(mid);
            if (m != null) movies.add(m);
        }
        movies.sort(Comparator.comparing(a -> a.title));
        return movies;
    }

    public List<Theater> viewTheatersInCity(String cityId) {
        City city = citiesById.get(cityId);
        if (city == null) throw new IllegalArgumentException("Unknown cityId: " + cityId);
        List<Theater> res = new ArrayList<>(city.theatersById.values());
        res.sort(Comparator.comparing(a -> a.name));
        return res;
    }

    public List<Show> viewShowsForMovie(String cityId, String movieId) {
        City city = citiesById.get(cityId);
        if (city == null) throw new IllegalArgumentException("Unknown cityId: " + cityId);

        List<Show> res = new ArrayList<>();
        for (Theater theater : city.theatersById.values()) {
            for (Screen screen : theater.screensById.values()) {
                for (Show show : screen.showsById.values()) {
                    if (show.movieId.equals(movieId)) res.add(show);
                }
            }
        }
        res.sort(Comparator.comparing(a -> a.startTime));
        return res;
    }

    public List<Show> viewShowsForTheater(String cityId, String theaterId) {
        City city = citiesById.get(cityId);
        if (city == null) throw new IllegalArgumentException("Unknown cityId: " + cityId);
        Theater theater = city.theatersById.get(theaterId);
        if (theater == null) throw new IllegalArgumentException("Unknown theaterId in city: " + theaterId);

        List<Show> res = new ArrayList<>();
        for (Screen screen : theater.screensById.values()) {
            res.addAll(screen.showsById.values());
        }
        res.sort(Comparator.comparing(a -> a.startTime));
        return res;
    }

    public List<SeatSnapshot> viewSeatMap(String showId) {
        Show show = showsById.get(showId);
        if (show == null) throw new IllegalArgumentException("Unknown showId: " + showId);
        long nowMs = System.currentTimeMillis();
        return show.getSeatMap(nowMs);
    }

    public ReservationToken selectSeats(String showId, List<String> seatIds, String userId) {
        Show show = showsById.get(showId);
        if (show == null) throw new IllegalArgumentException("Unknown showId: " + showId);
        Objects.requireNonNull(userId, "userId");
        long nowMs = System.currentTimeMillis();
        return show.lockSeats(seatIds, userId, nowMs, seatLockDurationMs);
    }

    public BookingResult confirmBooking(ReservationToken token, PaymentMethod method) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(method, "method");

        Show show = showsById.get(token.showId);
        if (show == null) throw new IllegalArgumentException("Unknown showId in token: " + token.showId);

        Movie movie = moviesById.get(show.movieId);
        if (movie == null) throw new IllegalStateException("Missing movie for showId: " + show.showId + ", movieId=" + show.movieId);

        long nowMs = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        double totalAmount = pricingEngine.computeTotalPrice(show, movie, token.seatIds, now);
        PaymentResult payment = paymentGateway.pay(method, totalAmount, token.reservationId);

        if (payment.finalStatus == PaymentStatus.SUCCESS) {
            Booking booking = show.confirmBooking(token, payment.paymentId, totalAmount, nowMs);
            bookingsById.put(booking.bookingId, booking);
            return new BookingResult(payment.finalStatus, booking, payment.paymentId, payment.stateHistory, payment.message);
        }

        // Payment failed => release seats (as per requirements).
        show.releaseReservation(token);
        return new BookingResult(payment.finalStatus, null, payment.paymentId, payment.stateHistory, payment.message);
    }

    public Booking getBooking(String bookingId) {
        return bookingsById.get(bookingId);
    }

    public Booking cancelBooking(String bookingId) {
        Objects.requireNonNull(bookingId, "bookingId");
        Booking booking = bookingsById.get(bookingId);
        if (booking == null) throw new IllegalArgumentException("Unknown bookingId: " + bookingId);
        if (booking.status == BookingStatus.CANCELLED) return booking;

        Show show = showsById.get(booking.showId);
        if (show == null) throw new IllegalStateException("Missing show for bookingId: " + bookingId);

        LocalDateTime now = LocalDateTime.now();
        if (!show.startTime.isAfter(now)) {
            throw new IllegalStateException("Cancellation allowed only before show time");
        }

        // Atomic: release all seats together via Show.
        show.cancelBooking(booking);
        booking.status = BookingStatus.CANCELLED;

        // Refund trigger (mock).
        paymentGateway.refund(booking.paymentId);
        return booking;
    }

    // -------------------------
    // Helpers
    // -------------------------
    private Theater findTheaterById(String theaterId) {
        for (City city : citiesById.values()) {
            Theater theater = city.theatersById.get(theaterId);
            if (theater != null) return theater;
        }
        return null;
    }

    private Screen findScreenById(String screenId) {
        for (City city : citiesById.values()) {
            for (Theater theater : city.theatersById.values()) {
                Screen screen = theater.screensById.get(screenId);
                if (screen != null) return screen;
            }
        }
        return null;
    }
}

final class BookingResult {
    final PaymentStatus paymentStatus;
    final Booking booking; // null on payment failure
    final String paymentId;
    final List<PaymentStatus> paymentStateHistory;
    final String message;

    BookingResult(PaymentStatus paymentStatus, Booking booking, String paymentId, List<PaymentStatus> paymentStateHistory, String message) {
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "paymentStatus");
        this.booking = booking;
        this.paymentId = Movie.requireNonBlank(paymentId, "paymentId");
        this.paymentStateHistory = List.copyOf(paymentStateHistory);
        this.message = message == null ? "" : message;
    }

    boolean isSuccess() {
        return paymentStatus == PaymentStatus.SUCCESS && booking != null;
    }
}

