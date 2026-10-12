package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.PaymentRequest;
import dev.rupom.project.airbnb.dto.PaymentResponse;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Payment;
import dev.rupom.project.airbnb.entity.enums.BookingStatus;
import dev.rupom.project.airbnb.entity.enums.PaymentStatus;
import dev.rupom.project.airbnb.exception.BadRequestException;
import dev.rupom.project.airbnb.exception.ForbiddenException;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.PaymentMapper;
import dev.rupom.project.airbnb.repository.BookingRepository;
import dev.rupom.project.airbnb.repository.PaymentRepository;
import dev.rupom.project.airbnb.dto.event.BookingEvent;
import dev.rupom.project.airbnb.dto.event.BookingEventType;
import dev.rupom.project.airbnb.dto.event.PaymentEvent;
import dev.rupom.project.airbnb.dto.event.PaymentEventType;
import dev.rupom.project.airbnb.service.kafka.KafkaEventProducer;
import dev.rupom.project.airbnb.util.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;
    private final PayoutService payoutService;
    private final KafkaEventProducer kafkaEventProducer;
    private final PaymentIdempotencyService paymentIdempotencyService;

    private static final String BOOKING_NOT_FOUND = "Booking not found.";
    private static final String PAYMENT_NOT_FOUND = "Payment not found.";

    @Transactional
    @Override
    public PaymentResponse processPayment(Long userId, PaymentRequest request) {
        return processPayment(userId, request, null);
    }

    @Transactional
    @Override
    public PaymentResponse processPayment(Long userId, PaymentRequest request, String idempotencyKey) {
        log.info("Processing payment for booking id: {} by user id: {} (idempotency: {})",
                request.getBookingId(), userId, idempotencyKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var cached = paymentIdempotencyService.getCachedPayment(userId, idempotencyKey);
            if (cached.isPresent()) {
                log.info("Idempotent hit: returning cached payment for booking id: {}", request.getBookingId());
                return cached.get();
            }
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));

        validateBookingAccess(booking, userId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot process payment for a cancelled booking.");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking is already confirmed and paid.");
        }

        if (request.getAmount().compareTo(booking.getTotalPrice()) != 0) {
            throw new BadRequestException("Payment amount (" + request.getAmount()
                    + ") must match booking total price (" + booking.getTotalPrice() + ").");
        }

        Payment payment = booking.getPayment();
        if (payment == null) {
            payment = Payment.builder()
                    .booking(booking)
                    .price(booking.getTotalPrice())
                    .status(PaymentStatus.PENDING)
                    .build();
        }

        // Mock payment gateway success simulation
        String transactionId = "TXN-" + UUID.randomUUID();
        payment.setTransactionId(transactionId);
        payment.setPrice(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.CONFIRMED);

        Payment savedPayment = paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPayment(savedPayment);
        bookingRepository.save(booking);

        // Schedule escrow host payout (released 24h post-check-in)
        payoutService.createEscrowPayout(booking);

        // Publish Kafka PaymentEvent
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(PaymentEventType.CONFIRMED)
                .paymentId(savedPayment.getId())
                .bookingId(booking.getId())
                .transactionId(transactionId)
                .amount(savedPayment.getPrice())
                .paymentMethod(savedPayment.getPaymentMethod())
                .guestEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)
                .guestName(booking.getUser() != null ? booking.getUser().getName() : null)
                .timestamp(Instant.now())
                .build();
        kafkaEventProducer.publishPaymentEvent(paymentEvent);

        // Publish Kafka BookingEvent (CONFIRMED)
        BookingEvent bookingEvent = BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(BookingEventType.CONFIRMED)
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
                .build();
        kafkaEventProducer.publishBookingEvent(bookingEvent);

        log.info("Payment confirmed with transaction ID: {} for booking id: {}", transactionId, booking.getId());
        PaymentResponse response = paymentMapper.toPaymentResponse(savedPayment);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            paymentIdempotencyService.cachePayment(userId, idempotencyKey, response);
        }

        return response;
    }

    @Override
    public PaymentResponse getPaymentByBookingId(Long bookingId, Long userId) {
        log.info("Fetching payment details for booking id: {} by user id: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));

        validateBookingAccess(booking, userId);

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(PAYMENT_NOT_FOUND));

        return paymentMapper.toPaymentResponse(payment);
    }

    private void validateBookingAccess(Booking booking, Long userId) {
        boolean isOwner = booking.getUser().getId().equals(userId);
        boolean isAdminOrManager = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGER");

        if (!isOwner && !isAdminOrManager) {
            throw new ForbiddenException("You do not have permission to pay or view payment for this booking.");
        }
    }
}
