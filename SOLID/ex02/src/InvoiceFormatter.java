/**
 * Single job: turn an Invoice into the exact string to print/save.
 */
public class InvoiceFormatter {

    public String format(Invoice inv) {
        StringBuilder out = new StringBuilder();
        out.append("Invoice# ").append(inv.invoiceId).append("\n");
        out.append(inv.formattedLines);
        out.append(String.format("Subtotal: %.2f\n", inv.subtotal));
        out.append(String.format("Tax(%.0f%%): %.2f\n", inv.taxPct, inv.tax));
        out.append(String.format("Discount: -%.2f\n", inv.discount));
        out.append(String.format("TOTAL: %.2f\n", inv.total));
        return out.toString();
    }
}
