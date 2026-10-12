package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.dto.event.NotificationEvent;
import dev.rupom.project.airbnb.dto.event.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationEventListenerTest {

    private final NotificationEventListener listener = new NotificationEventListener();

    @Test
    void handleNotificationEvent_LogsWithoutThrowing() {
        NotificationEvent event = NotificationEvent.builder()
                .eventId("notif-1")
                .recipientEmail("user@example.com")
                .recipientName("Alice")
                .subject("Test Notification")
                .message("This is a test notification message.")
                .notificationType(NotificationType.BOOKING_CONFIRMED)
                .timestamp(Instant.now())
                .build();

        assertDoesNotThrow(() -> listener.handleNotificationEvent(event));
    }
}
