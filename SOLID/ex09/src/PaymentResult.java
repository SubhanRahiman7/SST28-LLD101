public class PaymentResult {
    public final boolean success;
    public final String message;

    public PaymentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}

