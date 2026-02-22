/**
 * One eligibility rule. Returns a reason string if the student fails this rule, else null.
 * New rules can be added by implementing this interface.
 */
public interface EligibilityRule {
    String check(StudentProfile profile);
}
