public class CgrRule implements EligibilityRule {
    private static final double MIN_CGR = 8.0;

    @Override
    public String check(StudentProfile profile) {
        if (profile.cgr < MIN_CGR) {
            return "CGR below 8.0";
        }
        return null;
    }
}
