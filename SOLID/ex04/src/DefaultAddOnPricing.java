import java.util.List;

/** Default add-on prices: MESS 1000, LAUNDRY 500, GYM 300. */
public class DefaultAddOnPricing implements AddOnPricing {
    @Override
    public Money totalForAddOns(List<AddOn> addOns) {
        double total = 0.0;
        for (AddOn a : addOns) {
            if (a == AddOn.MESS) total += 1000.0;
            else if (a == AddOn.LAUNDRY) total += 500.0;
            else if (a == AddOn.GYM) total += 300.0;
        }
        return new Money(total);
    }
}
