package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Inventory;
import dev.rupom.project.airbnb.entity.Room;
import dev.rupom.project.airbnb.entity.enums.BookingStatus;
import dev.rupom.project.airbnb.repository.BookingRepository;
import dev.rupom.project.airbnb.repository.InventoryRepository;
import dev.rupom.project.airbnb.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpiryServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private dev.rupom.project.airbnb.service.kafka.KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private ReservationExpiryServiceImpl reservationExpiryService;

    private Booking staleBooking;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        Hotel hotel = Hotel.builder().id(1L).name("Test Hotel").build();
        Room room = Room.builder().id(10L).hotel(hotel).type("Deluxe").build();

        staleBooking = Booking.builder()
                .id(100L)
                .hotel(hotel)
                .room(room)
                .roomsCount(2)
                .checkInDate(LocalDate.now().plusDays(2))
                .checkOutDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.RESERVED)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .build();

        inventory = Inventory.builder()
                .id(50L)
                .room(room)
                .date(LocalDate.now().plusDays(2))
                .totalCount(5)
                .bookedCount(2)
                .price(BigDecimal.valueOf(100.0))
                .build();
    }

    @Test
    void expireStaleReservations_expiresBookingAndReleasesInventory() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(BookingStatus.RESERVED), any(LocalDateTime.class)))
                .thenReturn(List.of(staleBooking));
        when(inventoryRepository.findByRoomIdAndDateRange(10L, staleBooking.getCheckInDate(), staleBooking.getCheckOutDate()))
                .thenReturn(List.of(inventory));

        int expiredCount = reservationExpiryService.expireStaleReservations();

        assertEquals(1, expiredCount);
        assertEquals(BookingStatus.CANCELLED, staleBooking.getStatus());
        assertEquals(0, inventory.getBookedCount()); // 2 - 2 = 0

        verify(inventoryRepository, times(1)).saveAll(any());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void expireStaleReservations_noStaleBookings_returnsZero() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(BookingStatus.RESERVED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int expiredCount = reservationExpiryService.expireStaleReservations();

        assertEquals(0, expiredCount);
        verify(inventoryRepository, never()).saveAll(any());
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
