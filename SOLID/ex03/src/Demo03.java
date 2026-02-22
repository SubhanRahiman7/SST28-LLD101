/**
 * Demo for Ex3 — Placement Eligibility (OCP).
 * Run: javac *.java && java Demo03
 */
public class Demo03 {
    public static void main(String[] args) {
        System.out.println("=== Placement Eligibility ===");
        StudentProfile s = new StudentProfile("23BCS1001", "Ayaan", 8.10, 72, 18, LegacyFlags.NONE);
        EligibilityEngine engine = new EligibilityEngine(new FakeEligibilityStore());
        engine.runAndPrint(s);
    }
}
