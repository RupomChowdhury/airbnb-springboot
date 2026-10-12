package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.config.KafkaConfig;
import dev.rupom.project.airbnb.dto.event.*;
import dev.rupom.project.airbnb.entity.enums.PaymentMethod;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventProducer kafkaEventProducer;

    @BeforeEach
    void setUp() {
        SendResult<String, Object> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0, 0, 0, 0, 0);
        lenient().when(sendResult.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        lenient().when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);
    }

    @Test
    void publishBookingEvent_Success() {
        BookingEvent event = BookingEvent.builder()
                .eventId("evt-1")
                .eventType(BookingEventType.INITIATED)
                .bookingId(100L)
                .hotelId(1L)
                .hotelName("Seaside Villa")
                .guestEmail("guest@example.com")
                .totalPrice(BigDecimal.valueOf(250.00))
                .checkInDate(LocalDate.now().plusDays(2))
                .checkOutDate(LocalDate.now().plusDays(5))
                .timestamp(Instant.now())
                .build();

        kafkaEventProducer.publishBookingEvent(event);

        verify(kafkaTemplate, times(1)).send(eq(KafkaConfig.BOOKING_TOPIC), eq("100"), eq(event));
    }

    @Test
    void publishPaymentEvent_Success() {
        PaymentEvent event = PaymentEvent.builder()
                .eventId("evt-2")
                .eventType(PaymentEventType.CONFIRMED)
                .paymentId(50L)
                .bookingId(100L)
                .transactionId("TXN-12345")
                .amount(BigDecimal.valueOf(250.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .guestEmail("guest@example.com")
                .timestamp(Instant.now())
                .build();

        kafkaEventProducer.publishPaymentEvent(event);

        verify(kafkaTemplate, times(1)).send(eq(KafkaConfig.PAYMENT_TOPIC), eq("100"), eq(event));
    }

    @Test
    void publishPayoutEvent_Success() {
        PayoutEvent event = PayoutEvent.builder()
                .eventId("evt-3")
                .eventType(PayoutEventType.RELEASED)
                .payoutId(20L)
                .bookingId(100L)
                .hostId(10L)
                .hostEmail("host@example.com")
                .amount(BigDecimal.valueOf(200.00))
                .releaseDate(LocalDate.now())
                .timestamp(Instant.now())
                .build();

        kafkaEventProducer.publishPayoutEvent(event);

        verify(kafkaTemplate, times(1)).send(eq(KafkaConfig.PAYOUT_TOPIC), eq("10"), eq(event));
    }

    @Test
    void publishNotificationEvent_Success() {
        NotificationEvent event = NotificationEvent.builder()
                .eventId("evt-4")
                .recipientEmail("user@example.com")
                .recipientName("Alice")
                .subject("Welcome to AirBNB")
                .message("Thanks for joining!")
                .notificationType(NotificationType.BOOKING_CONFIRMED)
                .timestamp(Instant.now())
                .build();

        kafkaEventProducer.publishNotificationEvent(event);

        verify(kafkaTemplate, times(1)).send(eq(KafkaConfig.NOTIFICATION_TOPIC), eq("user@example.com"), eq(event));
    }
}
