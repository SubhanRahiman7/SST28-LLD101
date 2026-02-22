/**
 * Demo for Ex8 — Club Admin (ISP). Each tool implements only its role interface.
 * Run: javac *.java && java Demo08
 */
public class Demo08 {
    public static void main(String[] args) {
        System.out.println("=== Club Admin ===");
        BudgetLedger ledger = new BudgetLedger();
        MinutesBook minutes = new MinutesBook();
        EventPlanner events = new EventPlanner();

        ClubConsole console = new ClubConsole(ledger, minutes, events);
        console.run();
    }
}
