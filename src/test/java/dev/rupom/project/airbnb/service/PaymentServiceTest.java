package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.PaymentRequest;
import dev.rupom.project.airbnb.dto.PaymentResponse;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Payment;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.entity.enums.BookingStatus;
import dev.rupom.project.airbnb.entity.enums.PaymentMethod;
import dev.rupom.project.airbnb.entity.enums.PaymentStatus;
import dev.rupom.project.airbnb.exception.BadRequestException;
import dev.rupom.project.airbnb.mapper.PaymentMapper;
import dev.rupom.project.airbnb.repository.BookingRepository;
import dev.rupom.project.airbnb.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PayoutService payoutService;

    @Mock
    private dev.rupom.project.airbnb.service.kafka.KafkaEventProducer kafkaEventProducer;

    @Mock
    private PaymentIdempotencyService paymentIdempotencyService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User user;
    private Booking booking;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@example.com").build();
        Hotel hotel = Hotel.builder().id(10L).name("Test Hotel").owner(user).build();
        booking = Booking.builder()
                .id(100L)
                .user(user)
                .hotel(hotel)
                .status(BookingStatus.RESERVED)
                .totalPrice(BigDecimal.valueOf(250.00))
                .build();
    }

    @Test
    void processPayment_success() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(100L)
                .amount(BigDecimal.valueOf(250.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .status(PaymentStatus.PENDING)
                .price(BigDecimal.valueOf(250.00))
                .build();
        booking.setPayment(payment);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.CONFIRMED)
                .price(BigDecimal.valueOf(250.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
        when(paymentMapper.toPaymentResponse(any(Payment.class))).thenReturn(mockResponse);

        PaymentResponse response = paymentService.processPayment(1L, request);

        assertNotNull(response);
        assertEquals(PaymentStatus.CONFIRMED, response.getStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void processPayment_amountMismatch_throwsBadRequestException() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(100L)
                .amount(BigDecimal.valueOf(200.00)) // Expected 250.00
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> paymentService.processPayment(1L, request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_alreadyConfirmed_throwsBadRequestException() {
        booking.setStatus(BookingStatus.CONFIRMED);

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(100L)
                .amount(BigDecimal.valueOf(250.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> paymentService.processPayment(1L, request));
    }

    @Test
    void processPayment_withCachedIdempotencyKey_returnsCachedResponse() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(100L)
                .amount(BigDecimal.valueOf(250.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        PaymentResponse cachedResponse = PaymentResponse.builder()
                .id(1L)
                .transactionId("TXN-CACHED-123")
                .status(PaymentStatus.CONFIRMED)
                .price(BigDecimal.valueOf(250.00))
                .build();

        when(paymentIdempotencyService.getCachedPayment(1L, "idem-key-1")).thenReturn(Optional.of(cachedResponse));

        PaymentResponse response = paymentService.processPayment(1L, request, "idem-key-1");

        assertNotNull(response);
        assertEquals("TXN-CACHED-123", response.getTransactionId());
        verify(bookingRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_withNewIdempotencyKey_cachesAfterSuccessfulPayment() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(100L)
                .amount(BigDecimal.valueOf(250.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(paymentIdempotencyService.getCachedPayment(1L, "idem-key-2")).thenReturn(Optional.empty());
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(1L)
                .status(PaymentStatus.CONFIRMED)
                .price(BigDecimal.valueOf(250.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
        when(paymentMapper.toPaymentResponse(any(Payment.class))).thenReturn(mockResponse);

        PaymentResponse response = paymentService.processPayment(1L, request, "idem-key-2");

        assertNotNull(response);
        verify(paymentIdempotencyService, times(1)).cachePayment(eq(1L), eq("idem-key-2"), eq(mockResponse));
    }
}
