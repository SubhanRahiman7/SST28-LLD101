/** Default tax rules (student 5%, staff 2%, others 8%). */
public class DefaultTaxRules implements TaxPolicy {
    @Override
    public double taxPercent(String customerType) {
        if ("student".equalsIgnoreCase(customerType)) return 5.0;
        if ("staff".equalsIgnoreCase(customerType)) return 2.0;
        return 8.0;
    }
}
