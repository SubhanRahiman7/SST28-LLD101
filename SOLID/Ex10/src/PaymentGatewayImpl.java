public class PaymentGatewayImpl implements PaymentGateway {
    @Override
    public String charge(String studentId, double amount) {
        return "TXN-9001";
    }
}
