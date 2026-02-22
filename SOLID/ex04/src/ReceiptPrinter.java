import java.util.List;

/** Single job: print the fee receipt. */
public class ReceiptPrinter {
    public void print(BookingRequest req, Money monthly, Money deposit) {
        System.out.println("Room: " + LegacyRoomTypes.nameOf(req.roomType) + " | AddOns: " + req.addOns);
        System.out.println("Monthly: " + monthly);
        System.out.println("Deposit: " + deposit);
        System.out.println("TOTAL DUE NOW: " + monthly.plus(deposit));
    }
}
