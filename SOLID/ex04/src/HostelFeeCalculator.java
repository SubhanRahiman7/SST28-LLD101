import java.util.List;
import java.util.Random;

/**
 * Orchestrates fee calculation using pricing components. No switch on room/addons in this class.
 */
public class HostelFeeCalculator {
    private final BookingRepository repo;
    private final RoomPricing roomPricing;
    private final AddOnPricing addOnPricing;
    private final ReceiptPrinter receiptPrinter;

    public HostelFeeCalculator(BookingRepository repo) {
        this.repo = repo;
        this.roomPricing = new DefaultRoomPricing();
        this.addOnPricing = new DefaultAddOnPricing();
        this.receiptPrinter = new ReceiptPrinter();
    }

    public void process(BookingRequest req) {
        Money base = roomPricing.baseMonthly(req.roomType);
        Money addOns = addOnPricing.totalForAddOns(req.addOns);
        Money monthly = base.plus(addOns);
        Money deposit = new Money(5000.00);

        receiptPrinter.print(req, monthly, deposit);

        String bookingId = "H-" + (7000 + new Random(1).nextInt(1000));
        repo.save(bookingId, req, monthly, deposit);
    }
}
