public class Demo09 {
    public static void main(String[] args) {
        System.out.println("=== DIP Demo (Ex9) ===");

        PaymentService s1 = new PaymentService(new RazorpayGateway());
        s1.pay("U1001", 499.0);

        PaymentService s2 = new PaymentService(new StripeGateway());
        s2.pay("U2002", 799.0);
    }
}

