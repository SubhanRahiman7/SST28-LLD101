public class PaymentService {
    private final PaymentGateway gateway;

    public PaymentService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public void pay(String userId, double amount) {
        PaymentRequest req = new PaymentRequest(userId, amount);
        PaymentResult res = gateway.charge(req);
        if (res.success) {
            System.out.println("Payment SUCCESS: " + res.message);
        } else {
            System.out.println("Payment FAILED: " + res.message);
        }
    }
}

