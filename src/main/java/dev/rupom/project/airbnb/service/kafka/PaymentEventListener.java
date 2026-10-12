package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.config.KafkaConfig;
import dev.rupom.project.airbnb.dto.event.NotificationEvent;
import dev.rupom.project.airbnb.dto.event.NotificationType;
import dev.rupom.project.airbnb.dto.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final KafkaEventProducer kafkaEventProducer;

    @KafkaListener(topics = KafkaConfig.PAYMENT_TOPIC, groupId = "airbnb-group")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received PaymentEvent [{}] for payment id: {}, booking id: {}",
                event.getEventType(), event.getPaymentId(), event.getBookingId());

        if (event.getEventType() == null) {
            log.warn("PaymentEvent missing eventType, skipping: {}", event);
            return;
        }

        switch (event.getEventType()) {
            case CONFIRMED -> handlePaymentConfirmed(event);
            case REFUNDED -> handlePaymentRefunded(event);
            case CANCELLED -> log.info("Payment cancelled event received for booking id: {}", event.getBookingId());
        }
    }

    private void handlePaymentConfirmed(PaymentEvent event) {
        if (event.getGuestEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getGuestEmail())
                    .recipientName(event.getGuestName() != null ? event.getGuestName() : "Valued Guest")
                    .subject("Payment Receipt - Booking #" + event.getBookingId())
                    .message("Thank you! We received your payment of $" + event.getAmount()
                            + " via " + event.getPaymentMethod() + ". Transaction ID: " + event.getTransactionId() + ".")
                    .notificationType(NotificationType.PAYMENT_RECEIPT)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    private void handlePaymentRefunded(PaymentEvent event) {
        if (event.getGuestEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getGuestEmail())
                    .recipientName(event.getGuestName() != null ? event.getGuestName() : "Valued Guest")
                    .subject("Refund Issued - Booking #" + event.getBookingId())
                    .message("A refund of $" + event.getAmount() + " has been processed for your booking #"
                            + event.getBookingId() + " back to your original payment method.")
                    .notificationType(NotificationType.BOOKING_CANCELLED)
                    .timestamp(Instant.now())
                    .build());
        }
    }
}
