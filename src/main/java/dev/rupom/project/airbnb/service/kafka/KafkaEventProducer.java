package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.config.KafkaConfig;
import dev.rupom.project.airbnb.dto.event.BookingEvent;
import dev.rupom.project.airbnb.dto.event.NotificationEvent;
import dev.rupom.project.airbnb.dto.event.PaymentEvent;
import dev.rupom.project.airbnb.dto.event.PayoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBookingEvent(BookingEvent event) {
        String key = event.getBookingId() != null ? event.getBookingId().toString() : UUID.randomUUID().toString();
        log.info("Publishing BookingEvent [{}] for booking id: {} to topic: {}",
                event.getEventType(), event.getBookingId(), KafkaConfig.BOOKING_TOPIC);
        sendSafely(KafkaConfig.BOOKING_TOPIC, key, event);
    }

    public void publishPaymentEvent(PaymentEvent event) {
        String key = event.getBookingId() != null ? event.getBookingId().toString() : event.getPaymentId().toString();
        log.info("Publishing PaymentEvent [{}] for payment id: {}, booking id: {} to topic: {}",
                event.getEventType(), event.getPaymentId(), event.getBookingId(), KafkaConfig.PAYMENT_TOPIC);
        sendSafely(KafkaConfig.PAYMENT_TOPIC, key, event);
    }

    public void publishPayoutEvent(PayoutEvent event) {
        String key = event.getHostId() != null ? event.getHostId().toString() : event.getPayoutId().toString();
        log.info("Publishing PayoutEvent [{}] for payout id: {}, host id: {} to topic: {}",
                event.getEventType(), event.getPayoutId(), event.getHostId(), KafkaConfig.PAYOUT_TOPIC);
        sendSafely(KafkaConfig.PAYOUT_TOPIC, key, event);
    }

    public void publishNotificationEvent(NotificationEvent event) {
        String key = event.getRecipientEmail() != null ? event.getRecipientEmail() : UUID.randomUUID().toString();
        log.info("Publishing NotificationEvent [{}] to recipient: {} to topic: {}",
                event.getNotificationType(), event.getRecipientEmail(), KafkaConfig.NOTIFICATION_TOPIC);
        sendSafely(KafkaConfig.NOTIFICATION_TOPIC, key, event);
    }

    private void sendSafely(String topic, String key, Object payload) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to deliver message to topic {} with key {}: {}", topic, key, ex.getMessage());
                } else if (result != null) {
                    log.debug("Successfully delivered message to topic {} [partition: {}, offset: {}]",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("Error while dispatching message to Kafka topic {}: {}", topic, ex.getMessage(), ex);
        }
    }
}
