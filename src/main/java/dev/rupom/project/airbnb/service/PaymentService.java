package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.PaymentRequest;
import dev.rupom.project.airbnb.dto.PaymentResponse;

public interface PaymentService {
    default PaymentResponse processPayment(Long userId, PaymentRequest request) {
        return processPayment(userId, request, null);
    }

    PaymentResponse processPayment(Long userId, PaymentRequest request, String idempotencyKey);
    PaymentResponse getPaymentByBookingId(Long bookingId, Long userId);
}
