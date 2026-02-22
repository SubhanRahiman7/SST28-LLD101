public class DisciplinaryFlagRule implements EligibilityRule {
    @Override
    public String check(StudentProfile profile) {
        if (profile.disciplinaryFlag != LegacyFlags.NONE) {
            return "disciplinary flag present";
        }
        return null;
    }
}
