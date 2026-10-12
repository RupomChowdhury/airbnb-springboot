package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.dto.event.NotificationEvent;
import dev.rupom.project.airbnb.dto.event.NotificationType;
import dev.rupom.project.airbnb.dto.event.PayoutEvent;
import dev.rupom.project.airbnb.dto.event.PayoutEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutEventListenerTest {

    @Mock
    private KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private PayoutEventListener payoutEventListener;

    @Test
    void handlePayoutEvent_Released_NotifiesHost() {
        PayoutEvent event = PayoutEvent.builder()
                .eventId("po-1")
                .eventType(PayoutEventType.RELEASED)
                .payoutId(5L)
                .bookingId(100L)
                .hostId(10L)
                .hostEmail("host@test.com")
                .hostName("Host Jane")
                .amount(BigDecimal.valueOf(450.00))
                .releaseDate(LocalDate.now())
                .timestamp(Instant.now())
                .build();

        payoutEventListener.handlePayoutEvent(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaEventProducer, times(1)).publishNotificationEvent(captor.capture());

        NotificationEvent notif = captor.getValue();
        assertEquals("host@test.com", notif.getRecipientEmail());
        assertEquals(NotificationType.PAYOUT_RELEASED, notif.getNotificationType());
        assertTrue(notif.getMessage().contains("450"));
    }
}
