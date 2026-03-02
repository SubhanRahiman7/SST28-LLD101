public class StripeGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(PaymentRequest request) {
        System.out.println("[Stripe] Charging " + request.userId + " amount=" + request.amount);
        return new PaymentResult(true, "STRIPE_OK");
    }
}

