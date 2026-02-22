import java.util.List;

/**
 * Abstraction for saving and listing students.
 * Onboarding does not depend on FakeDb directly; it depends on this interface.
 */
public interface StudentRepository {
    void save(StudentRecord record);
    int count();
    List<StudentRecord> all();
}
