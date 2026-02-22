/**
 * Result of sending: success or failure with message. No throwing from send().
 */
public class SendResult {
    public final boolean success;
    public final String errorMessage;

    public static SendResult ok() {
        return new SendResult(true, null);
    }

    public static SendResult fail(String message) {
        return new SendResult(false, message);
    }

    private SendResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
