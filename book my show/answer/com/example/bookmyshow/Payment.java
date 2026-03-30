package com.example.bookmyshow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

enum PaymentMethod {
    CARD,
    UPI,
    NETBANKING
}

enum PaymentStatus {
    INITIATED,
    SUCCESS,
    FAILED
}

final class PaymentResult {
    final String paymentId;
    final PaymentStatus finalStatus;
    final List<PaymentStatus> stateHistory;
    final String message;

    PaymentResult(String paymentId, PaymentStatus finalStatus, List<PaymentStatus> stateHistory, String message) {
        this.paymentId = Movie.requireNonBlank(paymentId, "paymentId");
        this.finalStatus = Objects.requireNonNull(finalStatus, "finalStatus");
        this.stateHistory = List.copyOf(stateHistory);
        this.message = message == null ? "" : message;
    }
}

interface PaymentGateway {
    PaymentResult pay(PaymentMethod method, double amount, String reservationId);

    // In a real integration this would be an external call.
    PaymentResult refund(String paymentId);
}

/**
 * Mock payment gateway:
 * - records payment state transitions
 * - can be forced to fail to demonstrate seat release on payment failure
 */
final class PaymentGatewayMock implements PaymentGateway {
    private volatile boolean forceNextPaymentFailure = false;

    public void forceNextPaymentFailure() {
        forceNextPaymentFailure = true;
    }

    @Override
    public PaymentResult pay(PaymentMethod method, double amount, String reservationId) {
        Objects.requireNonNull(method, "method");
        Movie.requireNonBlank(reservationId, "reservationId");
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");

        String paymentId = "PAY-" + UUID.randomUUID();
        List<PaymentStatus> history = new ArrayList<>();
        history.add(PaymentStatus.INITIATED);

        boolean shouldFail = forceNextPaymentFailure || amount == 0;
        // reset after first use
        forceNextPaymentFailure = false;

        PaymentStatus finalStatus = shouldFail ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
        history.add(finalStatus);
        String msg = shouldFail ? "Mock failure" : "Mock success";
        return new PaymentResult(paymentId, finalStatus, history, msg);
    }

    @Override
    public PaymentResult refund(String paymentId) {
        Movie.requireNonBlank(paymentId, "paymentId");
        List<PaymentStatus> history = new ArrayList<>();
        history.add(PaymentStatus.INITIATED);
        history.add(PaymentStatus.SUCCESS);
        return new PaymentResult(paymentId, PaymentStatus.SUCCESS, history, "Mock refund issued");
    }
}

