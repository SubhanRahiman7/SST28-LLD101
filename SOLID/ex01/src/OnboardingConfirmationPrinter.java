import java.util.List;

/**
 * Single job: print the confirmation block after a student is registered.
 * Does not decide what to save or validate; only formats and prints.
 */
public class OnboardingConfirmationPrinter {

    public void printSuccess(String studentId, int totalStudents, StudentRecord record) {
        System.out.println("OK: created student " + studentId);
        System.out.println("Saved. Total students: " + totalStudents);
        System.out.println("CONFIRMATION:");
        System.out.println(record);
    }

    public void printInput(String rawInput) {
        System.out.println("INPUT: " + rawInput);
    }

    public void printValidationErrors(List<String> errors) {
        System.out.println("ERROR: cannot register");
        for (String e : errors) {
            System.out.println("- " + e);
        }
    }
}
