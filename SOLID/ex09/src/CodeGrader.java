/** Abstraction for grading code. Pipeline depends on this interface. */
public interface CodeGrader {
    int grade(Submission s, Rubric r);
}
