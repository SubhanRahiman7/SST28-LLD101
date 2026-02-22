/**
 * Abstraction for tax calculation. New rules can be added without editing checkout.
 */
public interface TaxPolicy {
    double taxPercent(String customerType);
}
