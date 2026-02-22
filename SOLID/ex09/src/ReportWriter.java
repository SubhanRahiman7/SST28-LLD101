/** Abstraction for writing the report. Pipeline depends on this interface. */
public interface ReportWriter {
    String write(Submission s, int plagScore, int codeScore);
}
