/** Operations for treasurer: income and expense. */
public interface LedgerOperations {
    void addIncome(double amt, String note);
    void addExpense(double amt, String note);
}
