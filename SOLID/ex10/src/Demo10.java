public class Demo10 {
    public static void main(String[] args) {
        System.out.println("=== DIP Demo (Ex10) ===");

        UserSignupService consoleSvc = new UserSignupService(new ConsoleLogger());
        consoleSvc.signup("riya@sst.edu");
        consoleSvc.signup("bad-email");

        FileLogger fileLogger = new FileLogger();
        UserSignupService fileSvc = new UserSignupService(fileLogger);
        fileSvc.signup("student@sst.edu");
        fileSvc.signup(null);

        System.out.println("--- File logger dump ---");
        System.out.print(fileLogger.dump());
    }
}

