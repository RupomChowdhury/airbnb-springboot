package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.BookingRequest;
import dev.rupom.project.airbnb.dto.BookingResponse;
import dev.rupom.project.airbnb.dto.CancellationRefundResponse;
import dev.rupom.project.airbnb.dto.GuestRequest;
import dev.rupom.project.airbnb.dto.PriceBreakdownDto;
import dev.rupom.project.airbnb.entity.*;
import dev.rupom.project.airbnb.entity.enums.BookingStatus;
import dev.rupom.project.airbnb.entity.enums.Gender;
import dev.rupom.project.airbnb.entity.enums.PaymentStatus;
import dev.rupom.project.airbnb.exception.BadRequestException;
import dev.rupom.project.airbnb.mapper.BookingMapper;
import dev.rupom.project.airbnb.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private PricingService pricingService;

    @Mock
    private PayoutService payoutService;

    @Mock
    private dev.rupom.project.airbnb.service.kafka.KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User user;
    private Hotel hotel;
    private Room room;
    private Inventory inventory1;
    private Inventory inventory2;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Rupom")
                .email("rupom@example.com")
                .build();

        hotel = Hotel.builder()
                .id(1L)
                .name("Grand Hotel")
                .city("Dhaka")
                .active(true)
                .build();

        room = Room.builder()
                .id(10L)
                .hotel(hotel)
                .type("Deluxe")
                .basePrice(BigDecimal.valueOf(100.00))
                .totalCount(5)
                .capacity(2)
                .build();

        LocalDate today = LocalDate.now();
        inventory1 = Inventory.builder()
                .id(101L)
                .hotel(hotel)
                .room(room)
                .date(today)
                .totalCount(5)
                .bookedCount(0)
                .surgeFactor(BigDecimal.ONE)
                .price(BigDecimal.valueOf(100.00))
                .closed(false)
                .build();

        inventory2 = Inventory.builder()
                .id(102L)
                .hotel(hotel)
                .room(room)
                .date(today.plusDays(1))
                .totalCount(5)
                .bookedCount(0)
                .surgeFactor(BigDecimal.ONE)
                .price(BigDecimal.valueOf(100.00))
                .closed(false)
                .build();
    }

    @Test
    void initiateBooking_success() {
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(2);

        GuestRequest guestRequest = GuestRequest.builder()
                .name("Guest One")
                .gender(Gender.MALE)
                .age(28)
                .build();

        BookingRequest request = BookingRequest.builder()
                .hotelId(1L)
                .roomId(10L)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .roomsCount(1)
                .guests(Set.of(guestRequest))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(roomRepository.findByHotelIdAndId(1L, 10L)).thenReturn(room);
        when(inventoryRepository.findAndLockByRoomIdAndDateRange(10L, checkIn, checkOut))
                .thenReturn(List.of(inventory1, inventory2));
        when(guestRepository.save(any(Guest.class))).thenAnswer(i -> i.getArgument(0));
        when(pricingService.calculatePriceBreakdown(any(), any(), anyLong()))
                .thenReturn(PriceBreakdownDto.builder()
                        .nightlyTotal(BigDecimal.valueOf(200.00))
                        .cleaningFee(BigDecimal.ZERO)
                        .serviceFee(BigDecimal.valueOf(28.00))
                        .discountAmount(BigDecimal.ZERO)
                        .taxAmount(BigDecimal.valueOf(11.40))
                        .totalPrice(BigDecimal.valueOf(239.40))
                        .hostPayout(BigDecimal.valueOf(194.00))
                        .build());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(500L);
            return b;
        });

        BookingResponse mockResponse = BookingResponse.builder()
                .id(500L)
                .status(BookingStatus.RESERVED)
                .totalPrice(BigDecimal.valueOf(200.00))
                .build();
        when(bookingMapper.toBookingResponse(any(Booking.class))).thenReturn(mockResponse);

        BookingResponse response = bookingService.initiateBooking(1L, request);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(BookingStatus.RESERVED, response.getStatus());
        assertEquals(BigDecimal.valueOf(200.00), response.getTotalPrice());

        // Verify inventory booked count was incremented
        assertEquals(1, inventory1.getBookedCount());
        assertEquals(1, inventory2.getBookedCount());
        verify(inventoryRepository, times(1)).saveAll(any());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void initiateBooking_capacityExceeded_throwsBadRequestException() {
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(1);

        GuestRequest guest1 = GuestRequest.builder().name("G1").gender(Gender.MALE).build();
        GuestRequest guest2 = GuestRequest.builder().name("G2").gender(Gender.FEMALE).build();
        GuestRequest guest3 = GuestRequest.builder().name("G3").gender(Gender.OTHER).build();

        // 1 room has capacity 2, but 3 guests provided
        BookingRequest request = BookingRequest.builder()
                .hotelId(1L)
                .roomId(10L)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .roomsCount(1)
                .guests(Set.of(guest1, guest2, guest3))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(roomRepository.findByHotelIdAndId(1L, 10L)).thenReturn(room);

        assertThrows(BadRequestException.class, () -> bookingService.initiateBooking(1L, request));
    }

    @Test
    void cancelBooking_success_releasesInventory() {
        Booking booking = Booking.builder()
                .id(500L)
                .user(user)
                .hotel(hotel)
                .room(room)
                .roomsCount(2)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .status(BookingStatus.CONFIRMED)
                .payment(Payment.builder().id(99L).status(PaymentStatus.CONFIRMED).build())
                .build();

        inventory1.setBookedCount(2);
        inventory2.setBookedCount(2);

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(inventoryRepository.findByRoomIdAndDateRange(10L, booking.getCheckInDate(), booking.getCheckOutDate()))
                .thenReturn(List.of(inventory1, inventory2));
        when(pricingService.calculateCancellationRefund(any(), any()))
                .thenReturn(CancellationRefundResponse.builder()
                        .bookingId(500L)
                        .refundAmount(BigDecimal.valueOf(200.00))
                        .hostPayoutAmount(BigDecimal.ZERO)
                        .penaltyAmount(BigDecimal.ZERO)
                        .build());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toBookingResponse(any(Booking.class))).thenReturn(BookingResponse.builder()
                .id(500L)
                .status(BookingStatus.CANCELLED)
                .build());

        BookingResponse response = bookingService.cancelBooking(500L, 1L);

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(0, inventory1.getBookedCount());
        assertEquals(0, inventory2.getBookedCount());
        assertEquals(PaymentStatus.REFUNDED, booking.getPayment().getStatus());
        verify(inventoryRepository, times(1)).saveAll(any());
        verify(payoutService, times(1)).updatePayoutAmount(eq(500L), any());
    }

    @Test
    void acceptBookingRequest_success() {
        hotel.setOwner(user);
        Booking requestedBooking = Booking.builder()
                .id(700L)
                .hotel(hotel)
                .room(room)
                .user(User.builder().id(2L).name("Guest").build())
                .status(BookingStatus.REQUESTED)
                .build();

        when(bookingRepository.findById(700L)).thenReturn(Optional.of(requestedBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(bookingMapper.toBookingResponse(any(Booking.class))).thenReturn(BookingResponse.builder()
                .id(700L)
                .status(BookingStatus.RESERVED)
                .build());

        BookingResponse response = bookingService.acceptBookingRequest(1L, 700L, 1L);

        assertNotNull(response);
        assertEquals(BookingStatus.RESERVED, response.getStatus());
        verify(bookingRepository, times(1)).save(requestedBooking);
    }

    @Test
    void rejectBookingRequest_success_releasesInventory() {
        hotel.setOwner(user);
        Booking requestedBooking = Booking.builder()
                .id(701L)
                .hotel(hotel)
                .room(room)
                .roomsCount(1)
                .checkInDate(LocalDate.now().plusDays(3))
                .checkOutDate(LocalDate.now().plusDays(5))
                .user(User.builder().id(2L).name("Guest").build())
                .status(BookingStatus.REQUESTED)
                .build();

        when(bookingRepository.findById(701L)).thenReturn(Optional.of(requestedBooking));
        when(inventoryRepository.findByRoomIdAndDateRange(10L, requestedBooking.getCheckInDate(), requestedBooking.getCheckOutDate()))
                .thenReturn(List.of(inventory1));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(bookingMapper.toBookingResponse(any(Booking.class))).thenReturn(BookingResponse.builder()
                .id(701L)
                .status(BookingStatus.CANCELLED)
                .build());

        BookingResponse response = bookingService.rejectBookingRequest(1L, 701L, 1L);

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, requestedBooking.getStatus());
        verify(inventoryRepository, times(1)).saveAll(any());
        verify(bookingRepository, times(1)).save(requestedBooking);
    }
}
