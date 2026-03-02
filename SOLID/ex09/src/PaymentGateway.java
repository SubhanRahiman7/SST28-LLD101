public interface PaymentGateway {
    PaymentResult charge(PaymentRequest request);
}

