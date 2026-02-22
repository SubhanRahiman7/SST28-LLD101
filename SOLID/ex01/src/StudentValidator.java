import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Single job: check student fields and return a list of error messages.
 * Does not print anything; just returns the errors in a fixed order.
 */
public class StudentValidator {

    private static final List<String> ALLOWED_PROGRAMS = List.of("CSE", "AI", "SWE");

    public List<String> validate(Map<String, String> fields) {
        List<String> errors = new ArrayList<>();
        String name = fields.getOrDefault("name", "");
        String email = fields.getOrDefault("email", "");
        String phone = fields.getOrDefault("phone", "");
        String program = fields.getOrDefault("program", "");

        if (name.isBlank()) {
            errors.add("name is required");
        }
        if (email.isBlank() || !email.contains("@")) {
            errors.add("email is invalid");
        }
        if (phone.isBlank() || !phone.chars().allMatch(Character::isDigit)) {
            errors.add("phone is invalid");
        }
        if (!ALLOWED_PROGRAMS.contains(program)) {
            errors.add("program is invalid");
        }
        return errors;
    }
}
