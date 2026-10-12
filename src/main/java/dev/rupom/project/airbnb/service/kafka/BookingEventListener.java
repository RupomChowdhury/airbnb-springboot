package dev.rupom.project.airbnb.service.kafka;

import dev.rupom.project.airbnb.config.KafkaConfig;
import dev.rupom.project.airbnb.dto.event.BookingEvent;
import dev.rupom.project.airbnb.dto.event.NotificationEvent;
import dev.rupom.project.airbnb.dto.event.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingEventListener {

    private final KafkaEventProducer kafkaEventProducer;

    @KafkaListener(topics = KafkaConfig.BOOKING_TOPIC, groupId = "airbnb-group")
    public void handleBookingEvent(BookingEvent event) {
        log.info("Received BookingEvent [{}] for booking id: {}", event.getEventType(), event.getBookingId());

        if (event.getEventType() == null) {
            log.warn("BookingEvent missing eventType, skipping: {}", event);
            return;
        }

        switch (event.getEventType()) {
            case INITIATED -> handleInitiated(event);
            case REQUESTED -> handleRequested(event);
            case ACCEPTED -> handleAccepted(event);
            case REJECTED -> handleRejected(event);
            case CONFIRMED -> handleConfirmed(event);
            case CANCELLED -> handleCancelled(event);
            case EXPIRED -> handleExpired(event);
        }
    }

    private void handleInitiated(BookingEvent event) {
        if (event.getGuestEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getGuestEmail())
                    .recipientName(event.getGuestName())
                    .subject("Reservation Hold Created - " + event.getHotelName())
                    .message("Your reservation for " + event.getHotelName() + " is temporarily held. "
                            + "Please complete payment of $" + event.getTotalPrice() + " within 15 minutes.")
                    .notificationType(NotificationType.BOOKING_CREATED)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    private void handleRequested(BookingEvent event) {
        // Notify host of request
        if (event.getHostEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getHostEmail())
                    .recipientName("Host")
                    .subject("New Booking Request for " + event.getHotelName())
                    .message("Guest " + event.getGuestName() + " requested to book " + event.getHotelName()
                            + " from " + event.getCheckInDate() + " to " + event.getCheckOutDate()
                            + ". Please review and respond in your host dashboard.")
                    .notificationType(NotificationType.BOOKING_REQUESTED)
                    .timestamp(Instant.now())
                    .build());
        }

        // Notify guest of request submission
        if (event.getGuestEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getGuestEmail())
                    .recipientName(event.getGuestName())
                    .subject("Booking Request Submitted - " + event.getHotelName())
                    .message("Your booking request for " + event.getHotelName()
                            + " was sent to the host. You will receive an update once the host reviews your request.")
                    .notificationType(NotificationType.BOOKING_REQUESTED)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    private void handleAccepted(BookingEvent event) {
        if (event.getGuestEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getGuestEmail())
                    .recipientName(event.getGuestName())
                    .subject("Booking Request Approved! - " + event.getHotelName())
                    .message("Great news! The host accepted your booking for " + event.getHotelName()
                            + ". Please complete payment to confirm your stay.")
                    .notificationType(NotificationType.BOOKING_ACCEPTED)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    private void handleRejected(BookingEvent event) {
        if (event.getGuestEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getGuestEmail())
                    .recipientName(event.getGuestName())
                    .subject("Booking Request Declined - " + event.getHotelName())
                    .message("Unfortunately, the host was unable to accept your booking for " + event.getHotelName()
                            + ". Your reservation hold has been released.")
                    .notificationType(NotificationType.BOOKING_DECLINED)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    private void handleConfirmed(BookingEvent event) {
        if (event.getGuestEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getGuestEmail())
                    .recipientName(event.getGuestName())
                    .subject("Reservation Confirmed! - " + event.getHotelName())
                    .message("Your reservation at " + event.getHotelName() + " is confirmed! "
                            + "Check-in: " + event.getCheckInDate() + ", Check-out: " + event.getCheckOutDate()
                            + ". We hope you have a wonderful stay!")
                    .notificationType(NotificationType.BOOKING_CONFIRMED)
                    .timestamp(Instant.now())
                    .build());
        }

        if (event.getHostEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getHostEmail())
                    .recipientName("Host")
                    .subject("Confirmed Guest Reservation - " + event.getHotelName())
                    .message("Guest " + event.getGuestName() + " completed payment for booking #"
                            + event.getBookingId() + ". Check-in: " + event.getCheckInDate() + ".")
                    .notificationType(NotificationType.BOOKING_CONFIRMED)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    private void handleCancelled(BookingEvent event) {
        if (event.getGuestEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getGuestEmail())
                    .recipientName(event.getGuestName())
                    .subject("Booking Cancelled - " + event.getHotelName())
                    .message("Your booking #" + event.getBookingId() + " at " + event.getHotelName()
                            + " has been cancelled. Any applicable refund has been processed per the cancellation policy.")
                    .notificationType(NotificationType.BOOKING_CANCELLED)
                    .timestamp(Instant.now())
                    .build());
        }

        if (event.getHostEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getHostEmail())
                    .recipientName("Host")
                    .subject("Booking Cancelled by Guest - " + event.getHotelName())
                    .message("Booking #" + event.getBookingId() + " at " + event.getHotelName()
                            + " was cancelled. Room dates have been reopened.")
                    .notificationType(NotificationType.BOOKING_CANCELLED)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    private void handleExpired(BookingEvent event) {
        if (event.getGuestEmail() != null) {
            kafkaEventProducer.publishNotificationEvent(NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipientEmail(event.getGuestEmail())
                    .recipientName(event.getGuestName())
                    .subject("Booking Hold Expired - " + event.getHotelName())
                    .message("Your 15-minute hold on booking #" + event.getBookingId()
                            + " has expired and rooms were released.")
                    .notificationType(NotificationType.BOOKING_EXPIRED)
                    .timestamp(Instant.now())
                    .build());
        }
    }
}
