package com.example.payments;

import java.util.Objects;

public class OrderService {
    private final PaymentGateway gateway;

    public OrderService(PaymentGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public String charge(String customerId, int amountCents) {
        return gateway.charge(customerId, amountCents);
    }
}
