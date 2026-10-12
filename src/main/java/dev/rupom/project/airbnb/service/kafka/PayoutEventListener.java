package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.config.KafkaConfig;
import dev.rupom.project.airbnb.dto.event.NotificationEvent;
import dev.rupom.project.airbnb.dto.event.NotificationType;
import dev.rupom.project.airbnb.dto.event.PayoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayoutEventListener {

    private final KafkaEventProducer kafkaEventProducer;

    @KafkaListener(topics = KafkaConfig.PAYOUT_TOPIC, groupId = "airbnb-group")
    public void handlePayoutEvent(PayoutEvent event) {
        log.info("Received PayoutEvent [{}] for payout id: {}, host id: {}",
                event.getEventType(), event.getPayoutId(), event.getHostId());

        if (event.getEventType() == null) {
            log.warn("PayoutEvent missing eventType, skipping: {}", event);
            return;
        }

        switch (event.getEventType()) {
            case RELEASED -> handlePayoutReleased(event);
            case CREATED -> log.info("Escrow payout #{} created for host #{}, scheduled release: {}",
                    event.getPayoutId(), event.getHostId(), event.getReleaseDate());
            case CANCELLED -> log.info("Escrow payout #{} cancelled for host #{}",
                    event.getPayoutId(), event.getHostId());
        }
    }

    private void handlePayoutReleased(PayoutEvent event) {
        if (event.getHostEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getHostEmail())
                    .recipientName(event.getHostName() != null ? event.getHostName() : "Host")
                    .subject("Host Escrow Payout Released - $" + event.getAmount())
                    .message("Your payout of $" + event.getAmount() + " for booking #"
                            + event.getBookingId() + " has been released from escrow and dispatched to your account.")
                    .notificationType(NotificationType.PAYOUT_RELEASED)
                    .timestamp(Instant.now())
                    .build());
        }
    }
}
