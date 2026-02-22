import java.util.List;

/** Default discount rules (student: 10 if subtotal>=180; staff: 15 if lines>=3 else 5). */
public class DefaultDiscountRules implements DiscountPolicy {
    @Override
    public double discountAmount(String customerType, double subtotal, List<OrderLine> lines) {
        if ("student".equalsIgnoreCase(customerType)) {
            return subtotal >= 180.0 ? 10.0 : 0.0;
        }
        if ("staff".equalsIgnoreCase(customerType)) {
            return lines.size() >= 3 ? 15.0 : 5.0;
        }
        return 0.0;
    }
}
