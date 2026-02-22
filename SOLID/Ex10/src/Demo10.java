/**
 * Demo for Ex10 — Transport Booking (DIP). Dependencies injected from Main.
 * Run: javac *.java && java Demo10
 */
public class Demo10 {
    public static void main(String[] args) {
        System.out.println("=== Transport Booking ===");
        TripRequest req = new TripRequest("23BCS1010", new GeoPoint(12.97, 77.59), new GeoPoint(12.93, 77.62));

        DistanceCalculator dist = new DistanceCalculatorImpl();
        DriverAllocator alloc = new DriverAllocatorImpl();
        PaymentGateway pay = new PaymentGatewayImpl();

        TransportBookingService svc = new TransportBookingService(dist, alloc, pay);
        svc.book(req);
    }
}
