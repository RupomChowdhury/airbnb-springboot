package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.dto.event.BookingEvent;
import dev.rupom.project.airbnb.dto.event.BookingEventType;
import dev.rupom.project.airbnb.dto.event.NotificationEvent;
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
class BookingEventListenerTest {

    @Mock
    private KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private BookingEventListener bookingEventListener;

    @Test
    void handleBookingEvent_Initiated_DispatchesNotification() {
        BookingEvent event = BookingEvent.builder()
                .eventId("b-1")
                .eventType(BookingEventType.INITIATED)
                .bookingId(101L)
                .hotelName("Sunset Resort")
                .guestEmail("guest@test.com")
                .guestName("John Doe")
                .totalPrice(BigDecimal.valueOf(300.00))
                .timestamp(Instant.now())
                .build();

        bookingEventListener.handleBookingEvent(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaEventProducer, times(1)).publishNotificationEvent(captor.capture());

        NotificationEvent notif = captor.getValue();
        assertEquals("guest@test.com", notif.getRecipientEmail());
        assertTrue(notif.getSubject().contains("Reservation Hold Created"));
    }

    @Test
    void handleBookingEvent_Requested_NotifiesHostAndGuest() {
        BookingEvent event = BookingEvent.builder()
                .eventId("b-2")
                .eventType(BookingEventType.REQUESTED)
                .bookingId(102L)
                .hotelName("Alpine Chalet")
                .guestEmail("guest@test.com")
                .guestName("John Doe")
                .hostEmail("host@test.com")
                .checkInDate(LocalDate.now().plusDays(3))
                .checkOutDate(LocalDate.now().plusDays(6))
                .timestamp(Instant.now())
                .build();

        bookingEventListener.handleBookingEvent(event);

        verify(kafkaEventProducer, times(2)).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void handleBookingEvent_Accepted_NotifiesGuest() {
        BookingEvent event = BookingEvent.builder()
                .eventId("b-3")
                .eventType(BookingEventType.ACCEPTED)
                .bookingId(103L)
                .hotelName("Alpine Chalet")
                .guestEmail("guest@test.com")
                .guestName("John Doe")
                .timestamp(Instant.now())
                .build();

        bookingEventListener.handleBookingEvent(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaEventProducer, times(1)).publishNotificationEvent(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("Approved"));
    }

    @Test
    void handleBookingEvent_Rejected_NotifiesGuest() {
        BookingEvent event = BookingEvent.builder()
                .eventId("b-4")
                .eventType(BookingEventType.REJECTED)
                .bookingId(104L)
                .hotelName("Alpine Chalet")
                .guestEmail("guest@test.com")
                .guestName("John Doe")
                .timestamp(Instant.now())
                .build();

        bookingEventListener.handleBookingEvent(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaEventProducer, times(1)).publishNotificationEvent(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("Declined"));
    }

    @Test
    void handleBookingEvent_Confirmed_NotifiesGuestAndHost() {
        BookingEvent event = BookingEvent.builder()
                .eventId("b-5")
                .eventType(BookingEventType.CONFIRMED)
                .bookingId(105L)
                .hotelName("Beachside Condo")
                .guestEmail("guest@test.com")
                .guestName("John Doe")
                .hostEmail("host@test.com")
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(4))
                .timestamp(Instant.now())
                .build();

        bookingEventListener.handleBookingEvent(event);

        verify(kafkaEventProducer, times(2)).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void handleBookingEvent_Cancelled_NotifiesGuestAndHost() {
        BookingEvent event = BookingEvent.builder()
                .eventId("b-6")
                .eventType(BookingEventType.CANCELLED)
                .bookingId(106L)
                .hotelName("Beachside Condo")
                .guestEmail("guest@test.com")
                .hostEmail("host@test.com")
                .timestamp(Instant.now())
                .build();

        bookingEventListener.handleBookingEvent(event);

        verify(kafkaEventProducer, times(2)).publishNotificationEvent(any(NotificationEvent.class));
    }

    @Test
    void handleBookingEvent_Expired_NotifiesGuest() {
        BookingEvent event = BookingEvent.builder()
                .eventId("b-7")
                .eventType(BookingEventType.EXPIRED)
                .bookingId(107L)
                .hotelName("Beachside Condo")
                .guestEmail("guest@test.com")
                .timestamp(Instant.now())
                .build();

        bookingEventListener.handleBookingEvent(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaEventProducer, times(1)).publishNotificationEvent(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("Expired"));
    }
}
