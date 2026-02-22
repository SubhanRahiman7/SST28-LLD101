import java.util.List;
import java.util.Map;

/**
 * Single job: compute subtotal, tax, discount, total and line-item text from menu and order.
 */
public class InvoiceCalculator {
    private final Map<String, MenuItem> menu;
    private final TaxPolicy taxPolicy;
    private final DiscountPolicy discountPolicy;

    public InvoiceCalculator(Map<String, MenuItem> menu, TaxPolicy taxPolicy, DiscountPolicy discountPolicy) {
        this.menu = menu;
        this.taxPolicy = taxPolicy;
        this.discountPolicy = discountPolicy;
    }

    public Invoice compute(String invoiceId, String customerType, List<OrderLine> lines) {
        double subtotal = 0.0;
        StringBuilder lineText = new StringBuilder();
        for (OrderLine l : lines) {
            MenuItem item = menu.get(l.itemId);
            double lineTotal = item.price * l.qty;
            subtotal += lineTotal;
            lineText.append(String.format("- %s x%d = %.2f\n", item.name, l.qty, lineTotal));
        }
        double taxPct = taxPolicy.taxPercent(customerType);
        double tax = subtotal * (taxPct / 100.0);
        double discount = discountPolicy.discountAmount(customerType, subtotal, lines);
        double total = subtotal + tax - discount;
        return new Invoice(invoiceId, subtotal, taxPct, tax, discount, total, lineText.toString());
    }
}
