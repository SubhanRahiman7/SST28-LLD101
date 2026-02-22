import java.util.List;

/**
 * Abstraction for discount calculation. New discounts without editing checkout.
 */
public interface DiscountPolicy {
    double discountAmount(String customerType, double subtotal, List<OrderLine> lines);
}
