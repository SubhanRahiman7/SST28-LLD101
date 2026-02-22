import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates eligibility by running a list of rules. New rules = add a class and add to list.
 * No giant if/else; no editing this method to add a rule.
 */
public class EligibilityEngine {
    private final List<EligibilityRule> rules;
    private final EligibilityResultStore store;

    public EligibilityEngine(EligibilityResultStore store) {
        this.store = store;
        this.rules = new ArrayList<>();
        rules.add(new DisciplinaryFlagRule());
        rules.add(new CgrRule());
        rules.add(new AttendanceRule());
        rules.add(new CreditsRule());
    }

    public EligibilityEngineResult evaluate(StudentProfile profile) {
        List<String> reasons = new ArrayList<>();
        for (EligibilityRule rule : rules) {
            String reason = rule.check(profile);
            if (reason != null) {
                reasons.add(reason);
                break; // same as original: first failing rule only
            }
        }
        String status = reasons.isEmpty() ? "ELIGIBLE" : "NOT_ELIGIBLE";
        return new EligibilityEngineResult(status, reasons);
    }

    public void runAndPrint(StudentProfile profile) {
        ReportPrinter printer = new ReportPrinter();
        EligibilityEngineResult result = evaluate(profile);
        printer.print(profile, result);
        store.save(profile.rollNo, result.status);
    }
}
