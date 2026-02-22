import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates cafeteria billing only: uses calculator, formatter, and store.
 * Does not encode tax/discount rules or format strings itself.
 */
public class CafeteriaSystem {
    private final Map<String, MenuItem> menu = new LinkedHashMap<>();
    private final InvoiceCalculator calculator;
    private final InvoiceFormatter formatter;
    private final InvoiceStore store;
    private int invoiceSeq = 1000;

    public CafeteriaSystem(InvoiceStore store) {
        this.store = store;
        this.calculator = new InvoiceCalculator(
                menu,
                new DefaultTaxRules(),
                new DefaultDiscountRules()
        );
        this.formatter = new InvoiceFormatter();
    }

    public void addToMenu(MenuItem item) {
        menu.put(item.id, item);
    }

    public void checkout(String customerType, List<OrderLine> lines) {
        String invId = "INV-" + (++invoiceSeq);
        Invoice inv = calculator.compute(invId, customerType, lines);
        String printable = formatter.format(inv);
        System.out.print(printable);
        store.save(invId, printable);
        System.out.println("Saved invoice: " + invId + " (lines=" + store.countLines(invId) + ")");
    }
}
