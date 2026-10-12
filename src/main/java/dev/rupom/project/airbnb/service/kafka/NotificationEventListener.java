package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.config.KafkaConfig;
import dev.rupom.project.airbnb.dto.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationEventListener {

    @KafkaListener(topics = KafkaConfig.NOTIFICATION_TOPIC, groupId = "airbnb-group")
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("""
                
                ========================= [NOTIFICATION SERVICE] =========================
                Notification Type : {}
                Event ID          : {}
                Recipient         : {} <{}>
                Subject           : {}
                Timestamp         : {}
                Message           :
                {}
                =========================================================================""",
                event.getNotificationType(),
                event.getEventId(),
                event.getRecipientName() != null ? event.getRecipientName() : "User",
                event.getRecipientEmail(),
                event.getSubject(),
                event.getTimestamp(),
                event.getMessage());
    }
}
