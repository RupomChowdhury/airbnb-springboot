package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.event.BookingEvent;
import dev.rupom.project.airbnb.dto.event.BookingEventType;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Inventory;
import dev.rupom.project.airbnb.entity.Payment;
import dev.rupom.project.airbnb.entity.enums.BookingStatus;
import dev.rupom.project.airbnb.entity.enums.PaymentStatus;
import dev.rupom.project.airbnb.repository.BookingRepository;
import dev.rupom.project.airbnb.repository.InventoryRepository;
import dev.rupom.project.airbnb.repository.PaymentRepository;
import dev.rupom.project.airbnb.service.kafka.KafkaEventProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationExpiryServiceImpl implements ReservationExpiryService {

    private final BookingRepository bookingRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final KafkaEventProducer kafkaEventProducer;

    private static final int RESERVATION_HOLD_MINUTES = 15;

    @Scheduled(fixedRate = 60000) // runs every 60 seconds
    @Transactional
    @Override
    public int expireStaleReservations() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(RESERVATION_HOLD_MINUTES);
        List<Booking> staleBookings = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.RESERVED, threshold);

        if (staleBookings.isEmpty()) {
            return 0;
        }

        log.info("Found {} stale unconfirmed reservations to expire", staleBookings.size());

        for (Booking booking : staleBookings) {
            log.info("Expiring stale reservation hold for booking id: {}", booking.getId());

            // Release reserved inventories back to pool
            List<Inventory> inventories = inventoryRepository.findByRoomIdAndDateRange(
                    booking.getRoom().getId(), booking.getCheckInDate(), booking.getCheckOutDate()
            );

            for (Inventory inventory : inventories) {
                inventory.setBookedCount(Math.max(0, inventory.getBookedCount() - booking.getRoomsCount()));
            }
            inventoryRepository.saveAll(inventories);

            booking.setStatus(BookingStatus.CANCELLED);

            if (booking.getPayment() != null) {
                Payment payment = booking.getPayment();
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
            }

            bookingRepository.save(booking);
            log.info("Successfully expired booking id: {} and released {} rooms", booking.getId(), booking.getRoomsCount());

            // Publish Kafka BookingEvent (EXPIRED)
            kafkaEventProducer.publishBookingEvent(BookingEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(BookingEventType.EXPIRED)
                    .bookingId(booking.getId())
                    .hotelId(booking.getHotel() != null ? booking.getHotel().getId() : null)
                    .hotelName(booking.getHotel() != null ? booking.getHotel().getName() : null)
                    .roomId(booking.getRoom() != null ? booking.getRoom().getId() : null)
                    .guestId(booking.getUser() != null ? booking.getUser().getId() : null)
                    .guestName(booking.getUser() != null ? booking.getUser().getName() : null)
                    .guestEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)
                    .hostId(booking.getHotel() != null && booking.getHotel().getOwner() != null ? booking.getHotel().getOwner().getId() : null)
                    .hostEmail(booking.getHotel() != null && booking.getHotel().getOwner() != null ? booking.getHotel().getOwner().getEmail() : null)
                    .totalPrice(booking.getTotalPrice())
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .timestamp(Instant.now())
                    .build());
        }

        return staleBookings.size();
    }
}
