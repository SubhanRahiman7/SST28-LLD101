public class CreditsRule implements EligibilityRule {
    private static final int MIN_CREDITS = 20;

    @Override
    public String check(StudentProfile profile) {
        if (profile.earnedCredits < MIN_CREDITS) {
            return "credits below 20";
        }
        return null;
    }
}
