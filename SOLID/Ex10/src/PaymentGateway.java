/** Abstraction for charging payment. */
public interface PaymentGateway {
    String charge(String studentId, double amount);
}
