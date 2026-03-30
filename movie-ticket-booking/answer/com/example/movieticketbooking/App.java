package com.example.movieticketbooking;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Self-contained demo for Movie Theater Booking LLD.
 * The actual LLD types are implemented as package-private top-level classes in this same file.
 */
public final class App {
    public static void main(String[] args) {
        PaymentGateway gateway = new MockPaymentGateway(false);
        long holdDurationMillis = 5 * 60 * 1000L; // 5 minutes
        MovieBookingSystem system = new MovieBookingSystem(gateway, holdDurationMillis);

        AdminService admin = system.admin();

        // --------- Catalog setup ----------
        Movie movie = new Movie("m1", "Inception", 120);
        Theater theater = new Theater("t1", "PVR", "Bengaluru");
        Screen screen = new Screen("s1", "t1", 5, 8);

        admin.addOrUpdateMovie(movie);
        admin.addOrUpdateTheater(theater);
        admin.addScreen("t1", screen);

        long startTime = System.currentTimeMillis() + 2 * 60 * 60 * 1000L; // +2 hours
        admin.addShowForScreen(
                "s1",
                movie,
                "sh1",
                startTime,
                "IMAX",
                5,
                8
        );

        // --------- Pricing rules ----------
        List<PricingRule> rules = Arrays.asList(
                new ShowBasedPricingRule("IMAX", 1.30),
                // Weekend evening surcharge: Sat/Sun, 18:00..23:59
                new DayTimePricingRule(1.20, 18, 23, new DayOfWeek[]{DayOfWeek.SATURDAY, DayOfWeek.SUNDAY}),
                // Demand based surge
                new DemandBasedPricingRule()
        );
        admin.configurePricingRules("Bengaluru", rules);

        // --------- User flow ----------
        System.out.println("Movies in Bengaluru: " + system.listMoviesInCity("Bengaluru").get(0).title());

        SeatMapResponse map = system.viewSeatMap("sh1");
        System.out.println("Seat A-1 initial: " + findSeat(map, "A-1").status());

        SeatHold hold = system.holdSeats("sh1", "u1", Arrays.asList("A-1", "A-2"));
        System.out.println("Hold created: " + hold.holdId());

        Booking booking = system.checkout(hold, PaymentMethod.UPI);
        System.out.println("Booking status: " + booking.status() + ", payment: " + booking.paymentStatus());

        // Cancel before showtime, triggers refund.
        Booking cancelled = system.cancelBooking(booking.bookingId(), "u1");
        System.out.println("After cancel: " + cancelled.status());
    }

    private static SeatInfo findSeat(SeatMapResponse map, String seatId) {
        for (SeatInfo info : map.seats()) {
            if (info.seatId().equalsIgnoreCase(seatId)) {
                return info;
            }
        }
        throw new IllegalArgumentException("Seat not found in response: " + seatId);
    }
}

// ----------------------------
// Facade + Admin
// ----------------------------

final class MovieBookingSystem {
    private final Map<String, Movie> moviesById = new ConcurrentHashMap<>();
    private final Map<String, Theater> theatersById = new ConcurrentHashMap<>();
    private final Map<String, Screen> screensById = new ConcurrentHashMap<>();
    private final Map<String, Show> showsById = new ConcurrentHashMap<>();
    private final Map<String, Booking> bookingsById = new ConcurrentHashMap<>();
    private final Map<String, PricingEngine> pricingEngineByCityName = new ConcurrentHashMap<>();

    private final PaymentGateway paymentGateway;
    private final long holdDurationMillis;

    public MovieBookingSystem(PaymentGateway paymentGateway, long holdDurationMillis) {
        if (paymentGateway == null) {
            throw new IllegalArgumentException("paymentGateway cannot be null");
        }
        if (holdDurationMillis <= 0) {
            throw new IllegalArgumentException("holdDurationMillis must be > 0");
        }
        this.paymentGateway = paymentGateway;
        this.holdDurationMillis = holdDurationMillis;
        this.pricingEngineByCityName.put("__default__", new PricingEngine(Collections.emptyList()));
    }

    public AdminService admin() {
        return new AdminService(this);
    }

    // User operations
    public List<Movie> listMoviesInCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("cityName cannot be null/blank");
        }
        Set<String> movieIds = new HashSet<>();
        for (Show show : showsById.values()) {
            if (cityName.equalsIgnoreCase(show.cityName())) {
                movieIds.add(show.movieId());
            }
        }
        List<Movie> result = new ArrayList<>();
        for (String movieId : movieIds) {
            Movie m = moviesById.get(movieId);
            if (m != null) {
                result.add(m);
            }
        }
        return result;
    }

    public List<Theater> listTheatersInCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("cityName cannot be null/blank");
        }
        List<Theater> result = new ArrayList<>();
        for (Theater theater : theatersById.values()) {
            if (cityName.equalsIgnoreCase(theater.cityName())) {
                result.add(theater);
            }
        }
        return result;
    }

    public List<Show> listShowsForMovie(String movieId) {
        if (movieId == null || movieId.isBlank()) {
            throw new IllegalArgumentException("movieId cannot be null/blank");
        }
        List<Show> result = new ArrayList<>();
        for (Show show : showsById.values()) {
            if (movieId.equalsIgnoreCase(show.movieId())) {
                result.add(show);
            }
        }
        return result;
    }

    public List<Show> listShowsForTheater(String theaterId) {
        if (theaterId == null || theaterId.isBlank()) {
            throw new IllegalArgumentException("theaterId cannot be null/blank");
        }
        Set<String> screenIds = new HashSet<>();
        for (Screen screen : screensById.values()) {
            if (theaterId.equalsIgnoreCase(screen.theaterId())) {
                screenIds.add(screen.screenId());
            }
        }
        List<Show> result = new ArrayList<>();
        for (Show show : showsById.values()) {
            if (screenIds.contains(show.screenId())) {
                result.add(show);
            }
        }
        return result;
    }

    public SeatMapResponse viewSeatMap(String showId) {
        if (showId == null || showId.isBlank()) {
            throw new IllegalArgumentException("showId cannot be null/blank");
        }
        Show show = showsById.get(showId);
        if (show == null) {
            throw new IllegalArgumentException("Unknown showId: " + showId);
        }
        return show.getSeatMap();
    }

    public SeatHold holdSeats(String showId, String userId, List<String> seatIds) {
        Show show = requireShow(showId);
        return show.holdSeats(userId, seatIds);
    }

    public Booking checkout(SeatHold hold, PaymentMethod paymentMethod) {
        if (hold == null) {
            throw new IllegalArgumentException("hold cannot be null");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("paymentMethod cannot be null");
        }

        Show show = requireShow(hold.showId());
        long now = System.currentTimeMillis();

        PricingEngine pricingEngine = pricingEngineByCityName.getOrDefault(
                show.cityName(),
                pricingEngineByCityName.get("__default__")
        );

        // Re-validate hold + compute demand-based pricing (under show lock).
        double total = show.priceForHold(pricingEngine, hold.holdId(), now);
        String bookingId = "bkg-" + UUID.randomUUID();
        Booking booking = new Booking(bookingId, hold, total, show.startTimeEpochMillis());

        PaymentRequest paymentRequest = new PaymentRequest(bookingId, total, paymentMethod, "INR");
        PaymentResult paymentResult = paymentGateway.charge(paymentRequest);

        if (paymentResult.status() == PaymentStatus.SUCCESS) {
            try {
                show.confirmHold(hold.holdId(), bookingId);
                booking.markPaymentSuccess(paymentResult.paymentId());
                bookingsById.put(bookingId, booking);
                return booking;
            } catch (RuntimeException ex) {
                // Seats changed after payment; release hold and treat as failed booking.
                show.releaseHold(hold.holdId());
                booking.markPaymentFailed(paymentResult.paymentId());
                bookingsById.put(bookingId, booking);
                throw ex;
            }
        }

        // Payment failed => release hold.
        show.releaseHold(hold.holdId());
        booking.markPaymentFailed(paymentResult.paymentId());
        bookingsById.put(bookingId, booking);
        return booking;
    }

    public Booking cancelBooking(String bookingId, String userId) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId cannot be null/blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null/blank");
        }

        Booking booking = bookingsById.get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Unknown bookingId: " + bookingId);
        }
        if (!userId.equalsIgnoreCase(booking.userId())) {
            throw new IllegalStateException("Booking does not belong to userId: " + userId);
        }
        if (booking.status() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED bookings can be cancelled");
        }
        long now = System.currentTimeMillis();
        if (now >= booking.showStartEpochMillis()) {
            throw new IllegalStateException("Cannot cancel after show time");
        }

        Show show = requireShow(booking.showId());

        // Release seats first (atomic seat consistency), then refund.
        show.cancelBooking(bookingId);
        boolean refunded = paymentGateway.refund(booking.paymentId(), booking.totalAmount());
        booking.markCancelled();
        if (refunded) {
            booking.markRefunded();
        }
        return booking;
    }

    // Admin internal operations
    void upsertMovie(Movie movie) {
        moviesById.put(movie.movieId(), movie);
    }

    void upsertTheater(Theater theater) {
        theatersById.put(theater.theaterId(), theater);
    }

    void upsertScreen(Screen screen) {
        screensById.put(screen.screenId(), screen);
    }

    void upsertShow(Show show) {
        showsById.put(show.showId(), show);
    }

    void configurePricingEngine(String cityName, PricingEngine engine) {
        pricingEngineByCityName.put(cityName, engine);
    }

    long holdDurationMillis() {
        return holdDurationMillis;
    }

    Screen requireScreen(String screenId) {
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("screenId cannot be null/blank");
        }
        Screen screen = screensById.get(screenId);
        if (screen == null) {
            throw new IllegalArgumentException("Unknown screenId: " + screenId);
        }
        return screen;
    }

    Theater requireTheater(String theaterId) {
        if (theaterId == null || theaterId.isBlank()) {
            throw new IllegalArgumentException("theaterId cannot be null/blank");
        }
        Theater theater = theatersById.get(theaterId);
        if (theater == null) {
            throw new IllegalArgumentException("Unknown theaterId: " + theaterId);
        }
        return theater;
    }

    Show requireShow(String showId) {
        Show show = showsById.get(showId);
        if (show == null) {
            throw new IllegalArgumentException("Unknown showId: " + showId);
        }
        return show;
    }
}

final class AdminService {
    private final MovieBookingSystem system;

    public AdminService(MovieBookingSystem system) {
        if (system == null) {
            throw new IllegalArgumentException("system cannot be null");
        }
        this.system = system;
    }

    public void addOrUpdateMovie(Movie movie) {
        if (movie == null) {
            throw new IllegalArgumentException("movie cannot be null");
        }
        system.upsertMovie(movie);
    }

    public void addOrUpdateTheater(Theater theater) {
        if (theater == null) {
            throw new IllegalArgumentException("theater cannot be null");
        }
        system.upsertTheater(theater);
    }

    public void addScreen(String theaterId, Screen screen) {
        if (theaterId == null || theaterId.isBlank()) {
            throw new IllegalArgumentException("theaterId cannot be null/blank");
        }
        if (screen == null) {
            throw new IllegalArgumentException("screen cannot be null");
        }
        if (!theaterId.equalsIgnoreCase(screen.theaterId())) {
            throw new IllegalArgumentException("screen.theaterId does not match theaterId");
        }
        system.upsertScreen(screen);
    }

    public void addShowForScreen(
            String screenId,
            Movie movie,
            String showId,
            long startTimeEpochMillis,
            String showType,
            int rows,
            int cols
    ) {
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("screenId cannot be null/blank");
        }
        if (movie == null) {
            throw new IllegalArgumentException("movie cannot be null");
        }
        if (showId == null || showId.isBlank()) {
            throw new IllegalArgumentException("showId cannot be null/blank");
        }
        if (showType == null || showType.isBlank()) {
            throw new IllegalArgumentException("showType cannot be null/blank");
        }

        Screen screen = system.requireScreen(screenId);
        Theater theater = system.requireTheater(screen.theaterId());

        Show show = new Show(
                showId,
                movie.movieId(),
                screenId,
                theater.cityName(),
                startTimeEpochMillis,
                showType,
                rows,
                cols,
                system.holdDurationMillis()
        );
        system.upsertShow(show);
    }

    public void configurePricingRules(String cityName, List<PricingRule> rules) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("cityName cannot be null/blank");
        }
        system.configurePricingEngine(cityName, new PricingEngine(rules));
    }
}

// ----------------------------
// Catalog models
// ----------------------------

final class Movie {
    private final String movieId;
    private final String title;
    private final int durationMinutes;

    public Movie(String movieId, String title, int durationMinutes) {
        if (movieId == null || movieId.isBlank()) {
            throw new IllegalArgumentException("movieId cannot be null/blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title cannot be null/blank");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be > 0");
        }
        this.movieId = movieId;
        this.title = title;
        this.durationMinutes = durationMinutes;
    }

    public String movieId() {
        return movieId;
    }

    public String title() {
        return title;
    }

    public int durationMinutes() {
        return durationMinutes;
    }
}

final class Theater {
    private final String theaterId;
    private final String name;
    private final String cityName;

    public Theater(String theaterId, String name, String cityName) {
        if (theaterId == null || theaterId.isBlank()) {
            throw new IllegalArgumentException("theaterId cannot be null/blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null/blank");
        }
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("cityName cannot be null/blank");
        }
        this.theaterId = theaterId;
        this.name = name;
        this.cityName = cityName;
    }

    public String theaterId() {
        return theaterId;
    }

    public String name() {
        return name;
    }

    public String cityName() {
        return cityName;
    }
}

final class Screen {
    private final String screenId;
    private final String theaterId;
    private final int rows;
    private final int cols;

    public Screen(String screenId, String theaterId, int rows, int cols) {
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("screenId cannot be null/blank");
        }
        if (theaterId == null || theaterId.isBlank()) {
            throw new IllegalArgumentException("theaterId cannot be null/blank");
        }
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("rows/cols must be > 0");
        }
        this.screenId = screenId;
        this.theaterId = theaterId;
        this.rows = rows;
        this.cols = cols;
    }

    public String screenId() {
        return screenId;
    }

    public String theaterId() {
        return theaterId;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }
}

// ----------------------------
// Seat / pricing / booking
// ----------------------------

enum SeatCategory {
    SILVER(100.0),
    GOLD(150.0),
    PLATINUM(220.0);

    private final double basePrice;

    SeatCategory(double basePrice) {
        this.basePrice = basePrice;
    }

    public double basePrice() {
        return basePrice;
    }
}

enum SeatStatus {
    AVAILABLE,
    HELD,
    BOOKED
}

final class Seat {
    private final String seatId;
    private final String rowLabel;
    private final int colNumber;
    private final SeatCategory category;
    private final double basePrice;

    private SeatStatus status;
    private String holdId;
    private String bookedBookingId;
    private long lockedUntilEpochMillis;

    public Seat(String seatId, SeatCategory category) {
        if (seatId == null || seatId.isBlank()) {
            throw new IllegalArgumentException("seatId cannot be null/blank");
        }
        if (category == null) {
            throw new IllegalArgumentException("category cannot be null");
        }

        int dashIdx = seatId.indexOf('-');
        if (dashIdx <= 0 || dashIdx >= seatId.length() - 1) {
            throw new IllegalArgumentException("seatId must look like 'A-1': " + seatId);
        }
        this.rowLabel = seatId.substring(0, dashIdx);
        String colPart = seatId.substring(dashIdx + 1);
        try {
            this.colNumber = Integer.parseInt(colPart);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("seatId col part must be numeric: " + seatId, ex);
        }
        if (this.colNumber <= 0) {
            throw new IllegalArgumentException("seatId col number must be > 0: " + seatId);
        }

        this.seatId = seatId;
        this.category = category;
        this.basePrice = category.basePrice();
        this.status = SeatStatus.AVAILABLE;
    }

    public String seatId() {
        return seatId;
    }

    public String rowLabel() {
        return rowLabel;
    }

    public int colNumber() {
        return colNumber;
    }

    public SeatCategory category() {
        return category;
    }

    public double basePrice() {
        return basePrice;
    }

    public SeatStatus status() {
        return status;
    }

    public String holdId() {
        return holdId;
    }

    public String bookedBookingId() {
        return bookedBookingId;
    }

    public long lockedUntilEpochMillis() {
        return lockedUntilEpochMillis;
    }

    public boolean isExpired(long nowEpochMillis) {
        return status == SeatStatus.HELD && lockedUntilEpochMillis > 0 && lockedUntilEpochMillis <= nowEpochMillis;
    }

    public boolean isHeldBy(String holdId) {
        return status == SeatStatus.HELD && this.holdId != null && this.holdId.equals(holdId);
    }

    void setAvailable() {
        status = SeatStatus.AVAILABLE;
        holdId = null;
        bookedBookingId = null;
        lockedUntilEpochMillis = 0;
    }

    void setHeld(String holdId, long lockedUntilEpochMillis) {
        if (holdId == null || holdId.isBlank()) {
            throw new IllegalArgumentException("holdId cannot be null/blank");
        }
        if (lockedUntilEpochMillis <= 0) {
            throw new IllegalArgumentException("lockedUntilEpochMillis must be > 0");
        }
        status = SeatStatus.HELD;
        this.holdId = holdId;
        this.lockedUntilEpochMillis = lockedUntilEpochMillis;
        bookedBookingId = null;
    }

    void setBooked(String bookingId) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId cannot be null/blank");
        }
        status = SeatStatus.BOOKED;
        bookedBookingId = bookingId;
        holdId = null;
        lockedUntilEpochMillis = 0;
    }
}

final class SeatInfo {
    private final String seatId;
    private final String rowLabel;
    private final int colNumber;
    private final SeatCategory category;
    private final SeatStatus status;
    private final double basePrice;

    public SeatInfo(String seatId, String rowLabel, int colNumber, SeatCategory category, SeatStatus status, double basePrice) {
        this.seatId = seatId;
        this.rowLabel = rowLabel;
        this.colNumber = colNumber;
        this.category = category;
        this.status = status;
        this.basePrice = basePrice;
    }

    public String seatId() {
        return seatId;
    }

    public String rowLabel() {
        return rowLabel;
    }

    public int colNumber() {
        return colNumber;
    }

    public SeatCategory category() {
        return category;
    }

    public SeatStatus status() {
        return status;
    }

    public double basePrice() {
        return basePrice;
    }
}

final class SeatMapResponse {
    private final String showId;
    private final int rows;
    private final int cols;
    private final List<SeatInfo> seats;

    public SeatMapResponse(String showId, int rows, int cols, List<SeatInfo> seats) {
        this.showId = showId;
        this.rows = rows;
        this.cols = cols;
        this.seats = Collections.unmodifiableList(new ArrayList<>(seats));
    }

    public String showId() {
        return showId;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public List<SeatInfo> seats() {
        return seats;
    }
}

final class SeatHold {
    private final String holdId;
    private final String showId;
    private final String userId;
    private final List<String> seatIds;
    private final long lockedUntilEpochMillis;

    public SeatHold(String holdId, String showId, String userId, List<String> seatIds, long lockedUntilEpochMillis) {
        if (holdId == null || holdId.isBlank()) {
            throw new IllegalArgumentException("holdId cannot be null/blank");
        }
        if (showId == null || showId.isBlank()) {
            throw new IllegalArgumentException("showId cannot be null/blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null/blank");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("seatIds cannot be null/empty");
        }
        if (lockedUntilEpochMillis <= 0) {
            throw new IllegalArgumentException("lockedUntilEpochMillis must be > 0");
        }

        this.holdId = holdId;
        this.showId = showId;
        this.userId = userId;
        this.seatIds = Collections.unmodifiableList(new ArrayList<>(seatIds));
        this.lockedUntilEpochMillis = lockedUntilEpochMillis;
    }

    public String holdId() {
        return holdId;
    }

    public String showId() {
        return showId;
    }

    public String userId() {
        return userId;
    }

    public List<String> seatIds() {
        return seatIds;
    }

    public long lockedUntilEpochMillis() {
        return lockedUntilEpochMillis;
    }
}

enum BookingStatus {
    INITIATED,
    CONFIRMED,
    FAILED,
    CANCELLED,
    REFUNDED
}

final class Booking {
    private final String bookingId;
    private final String holdId;
    private final String userId;
    private final String showId;
    private final long showStartEpochMillis;
    private final List<String> seatIds;
    private final double totalAmount;

    private volatile BookingStatus status;
    private volatile PaymentStatus paymentStatus;
    private volatile String paymentId;

    public Booking(String bookingId, SeatHold hold, double totalAmount, long showStartEpochMillis) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId cannot be null/blank");
        }
        if (hold == null) {
            throw new IllegalArgumentException("hold cannot be null");
        }
        if (totalAmount <= 0) {
            throw new IllegalArgumentException("totalAmount must be > 0");
        }

        this.bookingId = bookingId;
        this.holdId = hold.holdId();
        this.userId = hold.userId();
        this.showId = hold.showId();
        this.showStartEpochMillis = showStartEpochMillis;
        this.seatIds = Collections.unmodifiableList(new ArrayList<>(hold.seatIds()));
        this.totalAmount = totalAmount;

        this.status = BookingStatus.INITIATED;
        this.paymentStatus = PaymentStatus.INITIATED;
    }

    public String bookingId() {
        return bookingId;
    }

    public String holdId() {
        return holdId;
    }

    public String userId() {
        return userId;
    }

    public String showId() {
        return showId;
    }

    public long showStartEpochMillis() {
        return showStartEpochMillis;
    }

    public List<String> seatIds() {
        return seatIds;
    }

    public double totalAmount() {
        return totalAmount;
    }

    public BookingStatus status() {
        return status;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public String paymentId() {
        return paymentId;
    }

    synchronized void markPaymentSuccess(String paymentId) {
        this.paymentId = paymentId;
        this.paymentStatus = PaymentStatus.SUCCESS;
        this.status = BookingStatus.CONFIRMED;
    }

    synchronized void markPaymentFailed(String paymentId) {
        this.paymentId = paymentId;
        this.paymentStatus = PaymentStatus.FAILED;
        this.status = BookingStatus.FAILED;
    }

    synchronized void markCancelled() {
        if (status == BookingStatus.CONFIRMED) {
            status = BookingStatus.CANCELLED;
        }
    }

    synchronized void markRefunded() {
        if (status == BookingStatus.CANCELLED) {
            status = BookingStatus.REFUNDED;
        }
    }
}

// ----------------------------
// Payment
// ----------------------------

enum PaymentMethod {
    CARD,
    UPI,
    NETBANKING
}

enum PaymentStatus {
    INITIATED,
    SUCCESS,
    FAILED
}

final class PaymentRequest {
    private final String bookingId;
    private final double amount;
    private final PaymentMethod method;
    private final String currency;

    public PaymentRequest(String bookingId, double amount, PaymentMethod method, String currency) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId cannot be null/blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        if (method == null) {
            throw new IllegalArgumentException("method cannot be null");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency cannot be null/blank");
        }
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.currency = currency;
    }

    public String bookingId() {
        return bookingId;
    }

    public double amount() {
        return amount;
    }

    public PaymentMethod method() {
        return method;
    }

    public String currency() {
        return currency;
    }
}

final class PaymentResult {
    private final String paymentId;
    private final PaymentStatus status;
    private final String message;

    public PaymentResult(String paymentId, PaymentStatus status, String message) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("paymentId cannot be null/blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        this.paymentId = paymentId;
        this.status = status;
        this.message = message == null ? "" : message;
    }

    public String paymentId() {
        return paymentId;
    }

    public PaymentStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}

interface PaymentGateway {
    PaymentResult charge(PaymentRequest request);

    boolean refund(String paymentId, double amount);
}

final class MockPaymentGateway implements PaymentGateway {
    private final boolean forceFailure;

    public MockPaymentGateway(boolean forceFailure) {
        this.forceFailure = forceFailure;
    }

    @Override
    public PaymentResult charge(PaymentRequest request) {
        if (forceFailure) {
            return new PaymentResult("pay-" + UUID.randomUUID(), PaymentStatus.FAILED, "Simulated payment failure");
        }
        return new PaymentResult("pay-" + UUID.randomUUID(), PaymentStatus.SUCCESS, "Payment successful");
    }

    @Override
    public boolean refund(String paymentId, double amount) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("paymentId cannot be null/blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        return true;
    }
}

// ----------------------------
// Pricing
// ----------------------------

interface PricingRule {
    double multiplier(PricingContext context, SeatCategory category);
}

final class PricingEngine {
    private final List<PricingRule> rules;

    public PricingEngine(List<PricingRule> rules) {
        this.rules = rules == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(rules));
    }

    public double calculateTotal(Show show, List<String> seatIds, long nowEpochMillis) {
        if (show == null) {
            throw new IllegalArgumentException("show cannot be null");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("seatIds cannot be null/empty");
        }

        PricingContext context = new PricingContext(show, nowEpochMillis, show.effectiveDemandRatio(nowEpochMillis));
        double total = 0.0;
        for (String seatId : seatIds) {
            Seat seat = show.seatForPricing(seatId);
            double multiplier = 1.0;
            for (PricingRule rule : rules) {
                multiplier *= rule.multiplier(context, seat.category());
            }
            total += seat.basePrice() * multiplier;
        }
        return total;
    }
}

final class PricingContext {
    private final Show show;
    private final long nowEpochMillis;
    private final double demandRatio;

    PricingContext(Show show, long nowEpochMillis, double demandRatio) {
        this.show = show;
        this.nowEpochMillis = nowEpochMillis;
        this.demandRatio = demandRatio;
    }

    public Show show() {
        return show;
    }

    public long nowEpochMillis() {
        return nowEpochMillis;
    }

    public double demandRatio() {
        return demandRatio;
    }

    public DayOfWeek dayOfWeek() {
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowEpochMillis), show.zoneId());
        return ldt.getDayOfWeek();
    }

    public int hourOfDay() {
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowEpochMillis), show.zoneId());
        return ldt.getHour();
    }
}

final class ShowBasedPricingRule implements PricingRule {
    private final String matchShowType;
    private final double multiplier;

    public ShowBasedPricingRule(String matchShowType, double multiplier) {
        if (matchShowType == null || matchShowType.isBlank()) {
            throw new IllegalArgumentException("matchShowType cannot be null/blank");
        }
        if (multiplier <= 0) {
            throw new IllegalArgumentException("multiplier must be > 0");
        }
        this.matchShowType = matchShowType;
        this.multiplier = multiplier;
    }

    @Override
    public double multiplier(PricingContext context, SeatCategory category) {
        if (context == null || context.show() == null) {
            return 1.0;
        }
        String showType = context.show().showType();
        if (showType == null) {
            return 1.0;
        }
        return showType.equalsIgnoreCase(matchShowType) ? multiplier : 1.0;
    }
}

final class DayTimePricingRule implements PricingRule {
    private final double weekendEveningMultiplier;
    private final int fromHourInclusive;
    private final int toHourInclusive;
    private final DayOfWeek[] weekendDays;

    public DayTimePricingRule(double weekendEveningMultiplier, int fromHourInclusive, int toHourInclusive, DayOfWeek[] weekendDays) {
        if (weekendEveningMultiplier <= 0) {
            throw new IllegalArgumentException("weekendEveningMultiplier must be > 0");
        }
        if (fromHourInclusive < 0 || fromHourInclusive > 23) {
            throw new IllegalArgumentException("fromHourInclusive must be in [0,23]");
        }
        if (toHourInclusive < 0 || toHourInclusive > 23) {
            throw new IllegalArgumentException("toHourInclusive must be in [0,23]");
        }
        this.weekendEveningMultiplier = weekendEveningMultiplier;
        this.fromHourInclusive = fromHourInclusive;
        this.toHourInclusive = toHourInclusive;
        this.weekendDays = weekendDays == null ? new DayOfWeek[0] : weekendDays;
    }

    @Override
    public double multiplier(PricingContext context, SeatCategory category) {
        if (context == null) {
            return 1.0;
        }
        DayOfWeek day = context.dayOfWeek();
        int hour = context.hourOfDay();

        boolean isWeekend = false;
        for (DayOfWeek d : weekendDays) {
            if (d == day) {
                isWeekend = true;
                break;
            }
        }

        boolean isInRange = hour >= fromHourInclusive && hour <= toHourInclusive;
        if (isWeekend && isInRange) {
            return weekendEveningMultiplier;
        }
        return 1.0;
    }
}

final class DemandBasedPricingRule implements PricingRule {
    private final double t1;
    private final double t2;
    private final double t3;
    private final double m1;
    private final double m2;
    private final double m3;
    private final double m4;

    public DemandBasedPricingRule() {
        this(0.30, 0.60, 0.85, 1.0, 1.20, 1.50, 1.80);
    }

    public DemandBasedPricingRule(
            double t1,
            double t2,
            double t3,
            double m1,
            double m2,
            double m3,
            double m4
    ) {
        if (!(t1 >= 0 && t1 < t2 && t2 < t3 && t3 <= 1.0)) {
            throw new IllegalArgumentException("Thresholds must be 0<=t1<t2<t3<=1");
        }
        if (m1 <= 0 || m2 <= 0 || m3 <= 0 || m4 <= 0) {
            throw new IllegalArgumentException("Multipliers must be > 0");
        }
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.m1 = m1;
        this.m2 = m2;
        this.m3 = m3;
        this.m4 = m4;
    }

    @Override
    public double multiplier(PricingContext context, SeatCategory category) {
        if (context == null) {
            return 1.0;
        }
        double ratio = context.demandRatio();
        if (ratio < t1) {
            return m1;
        }
        if (ratio < t2) {
            return m2;
        }
        if (ratio < t3) {
            return m3;
        }
        return m4;
    }
}

// ----------------------------
// Show (seat holding + locking)
// ----------------------------

final class Show {
    private final String showId;
    private final String movieId;
    private final String screenId;
    private final String cityName;
    private final long startTimeEpochMillis;
    private final String showType;
    private final int rows;
    private final int cols;
    private final long holdDurationMillis;
    private final ZoneId zoneId;

    private final ReentrantLock seatLock = new ReentrantLock(true);
    private final Map<String, Seat> seatsById = new HashMap<>();
    private final Map<String, SeatHold> holdsById = new HashMap<>();
    private final Map<String, Set<String>> seatsByBookingId = new HashMap<>();

    public Show(
            String showId,
            String movieId,
            String screenId,
            String cityName,
            long startTimeEpochMillis,
            String showType,
            int rows,
            int cols,
            long holdDurationMillis
    ) {
        if (showId == null || showId.isBlank()) {
            throw new IllegalArgumentException("showId cannot be null/blank");
        }
        if (movieId == null || movieId.isBlank()) {
            throw new IllegalArgumentException("movieId cannot be null/blank");
        }
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("screenId cannot be null/blank");
        }
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("cityName cannot be null/blank");
        }
        if (startTimeEpochMillis <= 0) {
            throw new IllegalArgumentException("startTimeEpochMillis must be > 0");
        }
        if (showType == null || showType.isBlank()) {
            throw new IllegalArgumentException("showType cannot be null/blank");
        }
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("rows/cols must be > 0");
        }
        if (rows > 26) {
            throw new IllegalArgumentException("rows > 26 not supported in this LLD demo");
        }
        if (holdDurationMillis <= 0) {
            throw new IllegalArgumentException("holdDurationMillis must be > 0");
        }

        this.showId = showId;
        this.movieId = movieId;
        this.screenId = screenId;
        this.cityName = cityName;
        this.startTimeEpochMillis = startTimeEpochMillis;
        this.showType = showType;
        this.rows = rows;
        this.cols = cols;
        this.holdDurationMillis = holdDurationMillis;
        this.zoneId = ZoneId.systemDefault();

        buildSeats();
    }

    private void buildSeats() {
        int platinumRows = Math.max(1, rows / 3);
        int goldRows = Math.max(1, rows - platinumRows - (rows / 3));
        int silverRows = rows - platinumRows - goldRows;
        if (silverRows <= 0) {
            silverRows = 1;
            goldRows = rows - platinumRows - silverRows;
        }

        int platinumEndExclusive = platinumRows;
        int goldEndExclusive = platinumEndExclusive + goldRows;

        for (int r = 0; r < rows; r++) {
            char rowLabel = (char) ('A' + r);
            SeatCategory category;
            if (r < platinumEndExclusive) {
                category = SeatCategory.PLATINUM;
            } else if (r < goldEndExclusive) {
                category = SeatCategory.GOLD;
            } else {
                category = SeatCategory.SILVER;
            }
            for (int c = 1; c <= cols; c++) {
                String seatId = rowLabel + "-" + c;
                seatsById.put(seatId, new Seat(seatId, category));
            }
        }
    }

    public String showId() {
        return showId;
    }

    public String movieId() {
        return movieId;
    }

    public String screenId() {
        return screenId;
    }

    public String cityName() {
        return cityName;
    }

    public long startTimeEpochMillis() {
        return startTimeEpochMillis;
    }

    public String showType() {
        return showType;
    }

    ZoneId zoneId() {
        return zoneId;
    }

    Seat seatForPricing(String seatId) {
        Seat seat = seatsById.get(seatId);
        if (seat == null) {
            throw new IllegalArgumentException("Unknown seatId for show: " + seatId);
        }
        return seat;
    }

    double effectiveDemandRatio(long nowEpochMillis) {
        int totalSeats = rows * cols;
        int occupied = 0;
        for (Seat seat : seatsById.values()) {
            if (seat.status() == SeatStatus.BOOKED) {
                occupied++;
            } else if (seat.status() == SeatStatus.HELD && !seat.isExpired(nowEpochMillis)) {
                occupied++;
            }
        }
        return (double) occupied / (double) totalSeats;
    }

    public SeatMapResponse getSeatMap() {
        long now = System.currentTimeMillis();
        seatLock.lock();
        try {
            removeExpiredHolds(now);
            List<SeatInfo> infos = new ArrayList<>(rows * cols);
            for (int r = 0; r < rows; r++) {
                char rowLabel = (char) ('A' + r);
                for (int c = 1; c <= cols; c++) {
                    String seatId = rowLabel + "-" + c;
                    Seat seat = seatsById.get(seatId);
                    infos.add(new SeatInfo(seat.seatId(), String.valueOf(rowLabel), c, seat.category(), seat.status(), seat.basePrice()));
                }
            }
            return new SeatMapResponse(showId, rows, cols, infos);
        } finally {
            seatLock.unlock();
        }
    }

    public SeatHold holdSeats(String userId, List<String> requestedSeatIds) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null/blank");
        }
        if (requestedSeatIds == null || requestedSeatIds.isEmpty()) {
            throw new IllegalArgumentException("requestedSeatIds cannot be null/empty");
        }

        long now = System.currentTimeMillis();
        seatLock.lock();
        try {
            removeExpiredHolds(now);

            List<String> uniqueSeatIds = new ArrayList<>(requestedSeatIds.size());
            Set<String> seen = new HashSet<>();
            for (String seatId : requestedSeatIds) {
                if (seatId == null || seatId.isBlank()) {
                    throw new IllegalArgumentException("seatId cannot be null/blank");
                }
                if (!seen.add(seatId)) {
                    continue;
                }
                if (!seatsById.containsKey(seatId)) {
                    throw new IllegalArgumentException("Unknown seatId for show: " + seatId);
                }
                Seat seat = seatsById.get(seatId);
                if (seat.status() != SeatStatus.AVAILABLE) {
                    throw new IllegalStateException("Seat not available: " + seatId);
                }
                uniqueSeatIds.add(seatId);
            }
            if (uniqueSeatIds.isEmpty()) {
                throw new IllegalArgumentException("No valid seatIds after validation");
            }

            String holdId = "hold-" + UUID.randomUUID();
            long lockedUntil = now + holdDurationMillis;
            for (String seatId : uniqueSeatIds) {
                seatsById.get(seatId).setHeld(holdId, lockedUntil);
            }

            SeatHold hold = new SeatHold(holdId, showId, userId, uniqueSeatIds, lockedUntil);
            holdsById.put(holdId, hold);
            return hold;
        } finally {
            seatLock.unlock();
        }
    }

    public double priceForHold(PricingEngine pricingEngine, String holdId, long nowEpochMillis) {
        if (pricingEngine == null) {
            throw new IllegalArgumentException("pricingEngine cannot be null");
        }
        if (holdId == null || holdId.isBlank()) {
            throw new IllegalArgumentException("holdId cannot be null/blank");
        }

        seatLock.lock();
        try {
            removeExpiredHolds(nowEpochMillis);
            SeatHold hold = holdsById.get(holdId);
            if (hold == null) {
                throw new IllegalStateException("Hold not found or expired: " + holdId);
            }
            if (hold.lockedUntilEpochMillis() <= nowEpochMillis) {
                throw new IllegalStateException("Hold expired: " + holdId);
            }
            return pricingEngine.calculateTotal(this, hold.seatIds(), nowEpochMillis);
        } finally {
            seatLock.unlock();
        }
    }

    public void confirmHold(String holdId, String bookingId) {
        if (holdId == null || holdId.isBlank()) {
            throw new IllegalArgumentException("holdId cannot be null/blank");
        }
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId cannot be null/blank");
        }

        long now = System.currentTimeMillis();
        seatLock.lock();
        try {
            removeExpiredHolds(now);
            SeatHold hold = holdsById.get(holdId);
            if (hold == null) {
                throw new IllegalStateException("Hold not found or expired: " + holdId);
            }
            if (hold.lockedUntilEpochMillis() <= now) {
                throw new IllegalStateException("Hold expired: " + holdId);
            }

            for (String seatId : hold.seatIds()) {
                Seat seat = seatsById.get(seatId);
                if (seat == null || !seat.isHeldBy(holdId)) {
                    throw new IllegalStateException("Seat state changed for seatId=" + seatId);
                }
            }

            Set<String> bookedSeats = new HashSet<>(hold.seatIds());
            for (String seatId : bookedSeats) {
                seatsById.get(seatId).setBooked(bookingId);
            }
            holdsById.remove(holdId);
            seatsByBookingId.put(bookingId, bookedSeats);
        } finally {
            seatLock.unlock();
        }
    }

    public void releaseHold(String holdId) {
        if (holdId == null || holdId.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        seatLock.lock();
        try {
            removeExpiredHolds(now);
            SeatHold hold = holdsById.remove(holdId);
            if (hold == null) {
                return;
            }
            for (String seatId : hold.seatIds()) {
                Seat seat = seatsById.get(seatId);
                if (seat != null && seat.isHeldBy(holdId)) {
                    seat.setAvailable();
                }
            }
        } finally {
            seatLock.unlock();
        }
    }

    public void cancelBooking(String bookingId) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId cannot be null/blank");
        }

        long now = System.currentTimeMillis();
        seatLock.lock();
        try {
            removeExpiredHolds(now);
            Set<String> seatIds = seatsByBookingId.remove(bookingId);
            if (seatIds == null || seatIds.isEmpty()) {
                throw new IllegalStateException("No booked seats found for bookingId=" + bookingId);
            }
            for (String seatId : seatIds) {
                Seat seat = seatsById.get(seatId);
                if (seat == null || seat.status() != SeatStatus.BOOKED || !bookingId.equals(seat.bookedBookingId())) {
                    throw new IllegalStateException("Seat state inconsistent for cancellation seatId=" + seatId);
                }
                seat.setAvailable();
            }
        } finally {
            seatLock.unlock();
        }
    }

    private void removeExpiredHolds(long nowEpochMillis) {
        Iterator<Map.Entry<String, SeatHold>> it = holdsById.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SeatHold> entry = it.next();
            SeatHold hold = entry.getValue();
            if (hold.lockedUntilEpochMillis() <= nowEpochMillis) {
                for (String seatId : hold.seatIds()) {
                    Seat seat = seatsById.get(seatId);
                    if (seat != null && seat.isHeldBy(hold.holdId())) {
                        seat.setAvailable();
                    }
                }
                it.remove();
            }
        }
    }
}

