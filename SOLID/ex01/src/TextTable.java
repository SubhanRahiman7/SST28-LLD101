import java.util.List;

/**
 * Renders a simple text table of student records for the DB dump.
 */
public class TextTable {

    public static String render3(StudentRepository repo) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ID             | NAME | PROGRAM |\n");
        for (StudentRecord r : repo.all()) {
            sb.append(String.format("| %-14s | %-4s | %-7s |\n", r.id, r.name, r.program));
        }
        return sb.toString();
    }
}
