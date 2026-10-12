package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.dto.event.NotificationEvent;
import dev.rupom.project.airbnb.dto.event.NotificationType;
import dev.rupom.project.airbnb.dto.event.PaymentEvent;
import dev.rupom.project.airbnb.dto.event.PaymentEventType;
import dev.rupom.project.airbnb.entity.enums.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    @Test
    void handlePaymentEvent_Confirmed_DispatchesReceipt() {
        PaymentEvent event = PaymentEvent.builder()
                .eventId("p-1")
                .eventType(PaymentEventType.CONFIRMED)
                .paymentId(10L)
                .bookingId(100L)
                .transactionId("TXN-999")
                .amount(BigDecimal.valueOf(199.99))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .guestEmail("guest@test.com")
                .guestName("Bob")
                .timestamp(Instant.now())
                .build();

        paymentEventListener.handlePaymentEvent(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaEventProducer, times(1)).publishNotificationEvent(captor.capture());

        NotificationEvent notif = captor.getValue();
        assertEquals("guest@test.com", notif.getRecipientEmail());
        assertEquals(NotificationType.PAYMENT_RECEIPT, notif.getNotificationType());
        assertTrue(notif.getMessage().contains("TXN-999"));
    }

    @Test
    void handlePaymentEvent_Refunded_DispatchesRefundNotice() {
        PaymentEvent event = PaymentEvent.builder()
                .eventId("p-2")
                .eventType(PaymentEventType.REFUNDED)
                .paymentId(10L)
                .bookingId(100L)
                .transactionId("TXN-999")
                .amount(BigDecimal.valueOf(199.99))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .guestEmail("guest@test.com")
                .timestamp(Instant.now())
                .build();

        paymentEventListener.handlePaymentEvent(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaEventProducer, times(1)).publishNotificationEvent(captor.capture());

        NotificationEvent notif = captor.getValue();
        assertEquals("guest@test.com", notif.getRecipientEmail());
        assertTrue(notif.getSubject().contains("Refund Issued"));
    }
}
