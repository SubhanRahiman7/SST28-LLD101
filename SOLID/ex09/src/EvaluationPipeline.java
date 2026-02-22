/**
 * Depends only on abstractions (injected). No "new" for checker/grader/writer.
 */
public class EvaluationPipeline {
    private final PlagiarismChecker plagiarismChecker;
    private final CodeGrader codeGrader;
    private final ReportWriter reportWriter;

    public EvaluationPipeline(PlagiarismChecker plagiarismChecker, CodeGrader codeGrader, ReportWriter reportWriter) {
        this.plagiarismChecker = plagiarismChecker;
        this.codeGrader = codeGrader;
        this.reportWriter = reportWriter;
    }

    public void evaluate(Submission sub) {
        Rubric rubric = new Rubric();

        int plag = plagiarismChecker.check(sub);
        System.out.println("PlagiarismScore=" + plag);

        int code = codeGrader.grade(sub, rubric);
        System.out.println("CodeScore=" + code);

        String reportName = reportWriter.write(sub, plag, code);
        System.out.println("Report written: " + reportName);

        int total = plag + code;
        String result = (total >= 90) ? "PASS" : "FAIL";
        System.out.println("FINAL: " + result + " (total=" + total + ")");
    }
}
