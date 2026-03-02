public class PaymentRequest {
    public final String userId;
    public final double amount;

    public PaymentRequest(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }
}

