package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.PaymentRequest;
import dev.rupom.project.airbnb.dto.PaymentResponse;
import dev.rupom.project.airbnb.service.PaymentService;
import dev.rupom.project.airbnb.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid PaymentRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Attempting to process payment for booking id: {} by user: {} (idempotency: {})",
                request.getBookingId(), currentUserId, idempotencyKey);
        return ResponseEntity.ok(paymentService.processPayment(currentUserId, request, idempotencyKey));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable Long bookingId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Attempting to get payment for booking id: {} by user: {}", bookingId, currentUserId);
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId, currentUserId));
    }
}
