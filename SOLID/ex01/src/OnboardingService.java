import java.util.List;
import java.util.Map;

/**
 * Orchestrates the onboarding steps only. Uses parser, validator,
 * id generator, repository, and printer — does not do their jobs itself.
 */
public class OnboardingService {
    private final StudentRepository repository;
    private final RawInputParser parser;
    private final StudentValidator validator;
    private final StudentIdGenerator idGenerator;
    private final OnboardingConfirmationPrinter printer;

    public OnboardingService(StudentRepository repository) {
        this.repository = repository;
        this.parser = new RawInputParser();
        this.validator = new StudentValidator();
        this.idGenerator = new StudentIdGenerator();
        this.printer = new OnboardingConfirmationPrinter();
    }

    public void registerFromRawInput(String raw) {
        printer.printInput(raw);
        Map<String, String> fields = parser.parse(raw);

        List<String> errors = validator.validate(fields);
        if (!errors.isEmpty()) {
            printer.printValidationErrors(errors);
            return;
        }

        String id = idGenerator.nextId(repository.count());
        StudentRecord rec = new StudentRecord(
                id,
                fields.getOrDefault("name", ""),
                fields.getOrDefault("email", ""),
                fields.getOrDefault("phone", ""),
                fields.getOrDefault("program", "")
        );
        repository.save(rec);
        printer.printSuccess(id, repository.count(), rec);
    }
}
