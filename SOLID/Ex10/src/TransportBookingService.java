/**
 * Depends only on abstractions (injected). No "new" for distance/allocator/gateway.
 */
public class TransportBookingService {
    private final DistanceCalculator distanceCalculator;
    private final DriverAllocator driverAllocator;
    private final PaymentGateway paymentGateway;

    public TransportBookingService(DistanceCalculator distanceCalculator,
                                   DriverAllocator driverAllocator,
                                   PaymentGateway paymentGateway) {
        this.distanceCalculator = distanceCalculator;
        this.driverAllocator = driverAllocator;
        this.paymentGateway = paymentGateway;
    }

    public void book(TripRequest req) {
        double km = distanceCalculator.km(req.from, req.to);
        System.out.println("DistanceKm=" + km);

        String driver = driverAllocator.allocate(req.studentId);
        System.out.println("Driver=" + driver);

        double fare = 50.0 + km * 6.6666666667;
        fare = Math.round(fare * 100.0) / 100.0;

        String txn = paymentGateway.charge(req.studentId, fare);
        System.out.println("Payment=PAID txn=" + txn);

        BookingReceipt r = new BookingReceipt("R-501", fare);
        System.out.println("RECEIPT: " + r.id + " | fare=" + String.format("%.2f", r.fare));
    }
}
