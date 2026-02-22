import java.util.List;

/**
 * Abstraction for add-on total. New add-ons = new implementation or config, no switch edit.
 */
public interface AddOnPricing {
    Money totalForAddOns(List<AddOn> addOns);
}
