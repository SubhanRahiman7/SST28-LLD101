public class UserSignupService {
    private final Logger logger;

    public UserSignupService(Logger logger) {
        this.logger = logger;
    }

    public void signup(String email) {
        if (email == null || !email.contains("@")) {
            logger.error("Invalid email: " + email);
            return;
        }
        logger.info("Created account for " + email);
    }
}

