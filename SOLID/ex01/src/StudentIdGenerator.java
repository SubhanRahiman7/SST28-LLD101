/**
 * Single job: generate the next student ID given the current count in the store.
 */
public class StudentIdGenerator {

    public String nextId(int currentCount) {
        int next = currentCount + 1;
        String num = String.format("%04d", next);
        return "SST-2026-" + num;
    }
}
