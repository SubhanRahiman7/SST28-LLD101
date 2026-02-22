/** Abstraction for saving evaluation results. */
public interface EligibilityResultStore {
    void save(String rollNo, String status);
}
