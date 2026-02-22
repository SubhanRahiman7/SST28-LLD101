/**
 * Depends only on the small interfaces it needs: LedgerOperations, MinutesOperations, EventOperations.
 */
public class ClubConsole {
    private final LedgerOperations ledgerOps;
    private final MinutesOperations minutesOps;
    private final EventOperations eventOps;
    private final BudgetLedger ledger;
    private final MinutesBook minutes;

    public ClubConsole(BudgetLedger ledger, MinutesBook minutes, EventPlanner events) {
        this.ledger = ledger;
        this.minutes = minutes;
        this.ledgerOps = new TreasurerTool(ledger);
        this.minutesOps = new SecretaryTool(minutes);
        this.eventOps = new EventLeadTool(events);
    }

    public void run() {
        ledgerOps.addIncome(5000, "sponsor");
        minutesOps.addMinutes("Meeting at 5pm");
        eventOps.createEvent("HackNight", 2000);

        System.out.println("Summary: ledgerBalance=" + ledger.balanceInt()
                + ", minutes=" + minutes.count()
                + ", events=" + eventOps.getEventsCount());
    }
}
