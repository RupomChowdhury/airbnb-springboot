package dev.rupom.project.airbnb.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventId;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String message;
    private NotificationType notificationType;
    private Instant timestamp;
}
