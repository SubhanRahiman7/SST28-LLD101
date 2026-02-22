public class AttendanceRule implements EligibilityRule {
    private static final int MIN_ATTENDANCE = 75;

    @Override
    public String check(StudentProfile profile) {
        if (profile.attendancePct < MIN_ATTENDANCE) {
            return "attendance below 75";
        }
        return null;
    }
}
