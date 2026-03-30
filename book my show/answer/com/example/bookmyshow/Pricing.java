package com.example.bookmyshow;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pricing rules engine.
 * - Base price comes from seat category.
 * - Multipliers are applied by configurable rules (show-based, day/time, demand surge).
 */
interface PricingRule {
    double multiplier(PricingContext ctx);
}

final class PricingContext {
    final Show show;
    final Movie movie;
    final Seat seat;
    final LocalDateTime now;
    final double bookedRatio; // booked / totalSeats (0..1)

    PricingContext(Show show, Movie movie, Seat seat, LocalDateTime now, double bookedRatio) {
        this.show = show;
        this.movie = movie;
        this.seat = seat;
        this.now = now;
        this.bookedRatio = bookedRatio;
    }
}

final class PricingEngine {
    private final Map<SeatCategory, Double> basePricesByCategory = new HashMap<>();
    private final List<PricingRule> rules = new CopyOnWriteArrayList<>();

    public void setBasePrice(SeatCategory category, double basePrice) {
        Objects.requireNonNull(category, "category");
        if (basePrice < 0) throw new IllegalArgumentException("basePrice cannot be negative");
        basePricesByCategory.put(category, basePrice);
    }

    public void addRule(PricingRule rule) {
        rules.add(Objects.requireNonNull(rule, "rule"));
    }

    public double computeSeatPrice(Show show, Movie movie, Seat seat, LocalDateTime now) {
        Double base = basePricesByCategory.get(seat.category);
        if (base == null) throw new IllegalStateException("Missing base price for category: " + seat.category);

        double bookedRatio = showDemandRatio(show);
        PricingContext ctx = new PricingContext(show, movie, seat, now, bookedRatio);

        double price = base;
        for (PricingRule rule : rules) {
            price *= rule.multiplier(ctx);
        }
        return round2(price);
    }

    public double computeTotalPrice(Show show, Movie movie, List<String> seatIds, LocalDateTime now) {
        double total = 0;
        // Demand-based surge should consider the show current state. Seat locks are not counted as booked.
        for (String seatId : seatIds) {
            Seat seat = show.seatById(seatId);
            if (seat == null) throw new IllegalArgumentException("Unknown seatId: " + seatId);
            total += computeSeatPrice(show, movie, seat, now);
        }
        return round2(total);
    }

    private double showDemandRatio(Show show) {
        int total = show.totalSeats();
        if (total == 0) return 0.0;
        int booked = show.bookedSeatsCount();
        return Math.max(0.0, Math.min(1.0, booked / (double) total));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

final class ShowBasedPricingRule implements PricingRule {
    private final double multiplier;

    ShowBasedPricingRule(double multiplier) {
        if (multiplier <= 0) throw new IllegalArgumentException("multiplier must be > 0");
        this.multiplier = multiplier;
    }

    @Override
    public double multiplier(PricingContext ctx) {
        // Admin-configured movie/show tier multiplier.
        return ctx.movie.showTierMultiplier * multiplier;
    }
}

final class DayTimePricingRule implements PricingRule {
    private final double peakMultiplier;
    private final int peakStartHourInclusive;
    private final int peakEndHourExclusive;
    private final double weekendMultiplier;

    DayTimePricingRule(double peakMultiplier, int peakStartHourInclusive, int peakEndHourExclusive, double weekendMultiplier) {
        if (peakMultiplier <= 0 || weekendMultiplier <= 0) throw new IllegalArgumentException("multipliers must be > 0");
        this.peakMultiplier = peakMultiplier;
        this.peakStartHourInclusive = peakStartHourInclusive;
        this.peakEndHourExclusive = peakEndHourExclusive;
        this.weekendMultiplier = weekendMultiplier;
    }

    @Override
    public double multiplier(PricingContext ctx) {
        LocalDateTime showTime = ctx.show.startTime;
        DayOfWeek day = showTime.getDayOfWeek();
        boolean weekend = (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);

        int hour = showTime.getHour();
        boolean peak = hour >= peakStartHourInclusive && hour < peakEndHourExclusive;

        double m = 1.0;
        if (peak) m *= peakMultiplier;
        if (weekend) m *= weekendMultiplier;
        return m;
    }
}

final class DemandBasedSurgePricingRule implements PricingRule {
    private final double maxSurgeMultiplier; // e.g. 0.75 => up to 1.75x

    DemandBasedSurgePricingRule(double maxSurgeMultiplier) {
        if (maxSurgeMultiplier < 0) throw new IllegalArgumentException("maxSurgeMultiplier cannot be negative");
        this.maxSurgeMultiplier = maxSurgeMultiplier;
    }

    @Override
    public double multiplier(PricingContext ctx) {
        // bookedRatio in [0..1]. Price multiplier increases as seats get booked.
        // multiplier = 1 + bookedRatio * maxSurgeMultiplier
        return 1.0 + (ctx.bookedRatio * maxSurgeMultiplier);
    }
}

