/** Abstraction for plagiarism check. Pipeline depends on this, not a concrete class. */
public interface PlagiarismChecker {
    int check(Submission s);
}
