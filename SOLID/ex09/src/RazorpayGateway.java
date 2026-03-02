public class RazorpayGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(PaymentRequest request) {
        System.out.println("[Razorpay] Charging " + request.userId + " amount=" + request.amount);
        return new PaymentResult(true, "RAZORPAY_OK");
    }
}

