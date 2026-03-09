package com.example.payments;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class App {
    public static void main(String[] args) {
        Map<String, PaymentGateway> gateways = new HashMap<>();
        gateways.put("fastpay", new FastPayAdapter(new FastPayClient()));
        gateways.put("safecash", new SafeCashAdapter(new SafeCashClient()));

        String id1 = new OrderService(getGateway(gateways, "fastpay")).charge("cust-1", 1299);
        String id2 = new OrderService(getGateway(gateways, "safecash")).charge("cust-2", 1299);
        System.out.println(id1);
        System.out.println(id2);
    }

    private static PaymentGateway getGateway(Map<String, PaymentGateway> registry, String provider) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(provider, "provider");
        PaymentGateway gw = registry.get(provider);
        if (gw == null) throw new IllegalArgumentException("unknown provider: " + provider);
        return gw;
    }
}
