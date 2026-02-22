/** Abstraction for distance. Booking service depends on this, not a concrete class. */
public interface DistanceCalculator {
    double km(GeoPoint from, GeoPoint to);
}
