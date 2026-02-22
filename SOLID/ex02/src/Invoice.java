/** Result of computing an invoice (for formatting and saving). */
public class Invoice {
    public final String invoiceId;
    public final double subtotal;
    public final double taxPct;
    public final double tax;
    public final double discount;
    public final double total;
    public final String formattedLines; // line items text

    public Invoice(String invoiceId, double subtotal, double taxPct, double tax,
                   double discount, double total, String formattedLines) {
        this.invoiceId = invoiceId;
        this.subtotal = subtotal;
        this.taxPct = taxPct;
        this.tax = tax;
        this.discount = discount;
        this.total = total;
        this.formattedLines = formattedLines;
    }
}
