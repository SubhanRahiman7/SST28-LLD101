/**
 * Demo for Ex9 — Evaluation Pipeline (DIP). Dependencies injected from Main.
 * Run: javac *.java && java Demo09
 */
public class Demo09 {
    public static void main(String[] args) {
        System.out.println("=== Evaluation Pipeline ===");
        Submission sub = new Submission("23BCS1007", "public class A{}", "A.java");

        PlagiarismChecker checker = new PlagiarismCheckerImpl();
        CodeGrader grader = new CodeGraderImpl();
        ReportWriter writer = new ReportWriterImpl();

        EvaluationPipeline pipeline = new EvaluationPipeline(checker, grader, writer);
        pipeline.evaluate(sub);
    }
}
