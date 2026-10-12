package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.BookingRequest;
import dev.rupom.project.airbnb.dto.BookingResponse;
import dev.rupom.project.airbnb.dto.CancellationRefundResponse;
import dev.rupom.project.airbnb.dto.GuestRequest;
import dev.rupom.project.airbnb.dto.event.BookingEvent;
import dev.rupom.project.airbnb.dto.event.BookingEventType;
import dev.rupom.project.airbnb.entity.*;
import dev.rupom.project.airbnb.entity.enums.BookingStatus;
import dev.rupom.project.airbnb.entity.enums.PaymentStatus;
import dev.rupom.project.airbnb.exception.BadRequestException;
import dev.rupom.project.airbnb.exception.ForbiddenException;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.BookingMapper;
import dev.rupom.project.airbnb.repository.*;
import dev.rupom.project.airbnb.service.kafka.KafkaEventProducer;
import dev.rupom.project.airbnb.util.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final PaymentRepository paymentRepository;
    private final BookingMapper bookingMapper;
    private final PricingService pricingService;
    private final PayoutService payoutService;
    private final KafkaEventProducer kafkaEventProducer;

    private static final String HOTEL_NOT_FOUND = "Hotel not found.";
    private static final String ROOM_NOT_FOUND = "Room not found.";
    private static final String USER_NOT_FOUND = "User not found.";
    private static final String BOOKING_NOT_FOUND = "Booking not found.";

    @Transactional
    @Override
    public BookingResponse initiateBooking(Long userId, BookingRequest request) {
        log.info("Initiating booking for user id: {}, hotel id: {}, room id: {}", userId, request.getHotelId(), request.getRoomId());

        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));

        if (!Boolean.TRUE.equals(hotel.getActive())) {
            throw new BadRequestException("Cannot book a room in an inactive hotel.");
        }

        Room room = roomRepository.findByHotelIdAndId(request.getHotelId(), request.getRoomId());
        if (room == null) {
            throw new ResourceNotFoundException(ROOM_NOT_FOUND);
        }

        // Validate guest capacity
        int totalCapacity = room.getCapacity() * request.getRoomsCount();
        if (totalCapacity < request.getGuests().size()) {
            throw new BadRequestException("Room capacity exceeded. Max capacity for " + request.getRoomsCount()
                    + " rooms is " + totalCapacity + " guests.");
        }

        long numberOfNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());

        // Pessimistic write locking on inventories to prevent race conditions & double bookings
        List<Inventory> inventories = inventoryRepository.findAndLockByRoomIdAndDateRange(
                room.getId(), request.getCheckInDate(), request.getCheckOutDate()
        );

        if (inventories.size() != numberOfNights) {
            throw new BadRequestException("Inventory is not available for all selected dates.");
        }

        BigDecimal nightlyTotal = BigDecimal.ZERO;

        for (Inventory inventory : inventories) {
            if (Boolean.TRUE.equals(inventory.getClosed())) {
                throw new BadRequestException("Room is closed on " + inventory.getDate());
            }

            int availableRooms = inventory.getTotalCount() - inventory.getBookedCount();
            if (availableRooms < request.getRoomsCount()) {
                throw new BadRequestException("Not enough rooms available on " + inventory.getDate()
                        + ". Requested: " + request.getRoomsCount() + ", Available: " + availableRooms);
            }

            BigDecimal dailyCost = inventory.getPrice().multiply(BigDecimal.valueOf(request.getRoomsCount()));
            nightlyTotal = nightlyTotal.add(dailyCost);

            // Reserve inventory
            inventory.setBookedCount(inventory.getBookedCount() + request.getRoomsCount());
        }

        inventoryRepository.saveAll(inventories);

        // Calculate itemized price breakdown (cleaning fee, platform service fee, length of stay discount, tax, host payout)
        var breakdown = pricingService.calculatePriceBreakdown(hotel, nightlyTotal, numberOfNights);

        // Process guests
        Set<Guest> guests = new HashSet<>();
        for (GuestRequest guestRequest : request.getGuests()) {
            Guest guest = Guest.builder()
                    .user(user)
                    .name(guestRequest.getName())
                    .gender(guestRequest.getGender())
                    .age(guestRequest.getAge())
                    .build();
            guests.add(guestRepository.save(guest));
        }

        // Create pending payment record
        Payment payment = Payment.builder()
                .price(breakdown.getTotalPrice())
                .status(PaymentStatus.PENDING)
                .build();

        BookingStatus initialStatus = Boolean.FALSE.equals(hotel.getInstantBookable())
                ? BookingStatus.REQUESTED
                : BookingStatus.RESERVED;

        // Create booking in initial status (REQUESTED if host approval required, RESERVED if instant bookable)
        Booking booking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .user(user)
                .status(initialStatus)
                .roomsCount(request.getRoomsCount())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .nightlyTotal(breakdown.getNightlyTotal())
                .cleaningFee(breakdown.getCleaningFee())
                .serviceFee(breakdown.getServiceFee())
                .discountAmount(breakdown.getDiscountAmount())
                .taxAmount(breakdown.getTaxAmount())
                .totalPrice(breakdown.getTotalPrice())
                .hostPayout(breakdown.getHostPayout())
                .guests(guests)
                .payment(payment)
                .build();

        payment.setBooking(booking);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created successfully with id: {}, status: {} and total price: {}",
                savedBooking.getId(), savedBooking.getStatus(), savedBooking.getTotalPrice());

        // Publish Kafka event
        BookingEventType eventType = savedBooking.getStatus() == BookingStatus.REQUESTED
                ? BookingEventType.REQUESTED
                : BookingEventType.INITIATED;
        kafkaEventProducer.publishBookingEvent(toBookingEvent(savedBooking, eventType));

        return bookingMapper.toBookingResponse(savedBooking);
    }

    @Transactional
    @Override
    public BookingResponse cancelBooking(Long bookingId, Long userId) {
        log.info("Attempting to cancel booking id: {} by user id: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));

        validateBookingAccess(booking, userId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled.");
        }

        // Release inventory
        List<Inventory> inventories = inventoryRepository.findByRoomIdAndDateRange(
                booking.getRoom().getId(), booking.getCheckInDate(), booking.getCheckOutDate()
        );

        for (Inventory inventory : inventories) {
            inventory.setBookedCount(Math.max(0, inventory.getBookedCount() - booking.getRoomsCount()));
        }
        inventoryRepository.saveAll(inventories);

        // Calculate tiered refund and escrow host payout adjustment
        var refundBreakdown = pricingService.calculateCancellationRefund(booking, LocalDate.now());
        log.info("Cancellation refund breakdown: refund={}, penalty={}, hostPayout={}",
                refundBreakdown.getRefundAmount(), refundBreakdown.getPenaltyAmount(), refundBreakdown.getHostPayoutAmount());

        payoutService.updatePayoutAmount(booking.getId(), refundBreakdown.getHostPayoutAmount());

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);

        // Update payment status if present
        if (booking.getPayment() != null) {
            Payment payment = booking.getPayment();
            if (payment.getStatus() == PaymentStatus.CONFIRMED) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.CANCELLED);
            }
            paymentRepository.save(payment);
        }

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking id: {} cancelled successfully", bookingId);

        // Publish Kafka event
        kafkaEventProducer.publishBookingEvent(toBookingEvent(updatedBooking, BookingEventType.CANCELLED));

        return bookingMapper.toBookingResponse(updatedBooking);
    }

    @Override
    public CancellationRefundResponse getCancellationQuote(Long bookingId, Long userId) {
        log.info("Fetching cancellation refund quote for booking id: {} by user id: {}", bookingId, userId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));

        validateBookingAccess(booking, userId);
        return pricingService.calculateCancellationRefund(booking, LocalDate.now());
    }

    @Transactional
    @Override
    public BookingResponse acceptBookingRequest(Long hotelId, Long bookingId, Long hostUserId) {
        log.info("Host {} attempting to accept booking request id: {} for hotel id: {}", hostUserId, bookingId, hotelId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));

        validateHostAccess(booking.getHotel(), hostUserId);

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new BadRequestException("Booking is not in REQUESTED status. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.RESERVED);
        booking.setCreatedAt(LocalDateTime.now()); // Reset 15-min hold timer
        Booking saved = bookingRepository.save(booking);

        log.info("Booking request id: {} accepted. Status moved to RESERVED.", bookingId);

        // Publish Kafka event
        kafkaEventProducer.publishBookingEvent(toBookingEvent(saved, BookingEventType.ACCEPTED));

        return bookingMapper.toBookingResponse(saved);
    }

    @Transactional
    @Override
    public BookingResponse rejectBookingRequest(Long hotelId, Long bookingId, Long hostUserId) {
        log.info("Host {} attempting to reject booking request id: {} for hotel id: {}", hostUserId, bookingId, hotelId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));

        validateHostAccess(booking.getHotel(), hostUserId);

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new BadRequestException("Booking is not in REQUESTED status. Current status: " + booking.getStatus());
        }

        // Release inventory held during request
        List<Inventory> inventories = inventoryRepository.findByRoomIdAndDateRange(
                booking.getRoom().getId(), booking.getCheckInDate(), booking.getCheckOutDate()
        );
        for (Inventory inventory : inventories) {
            inventory.setBookedCount(Math.max(0, inventory.getBookedCount() - booking.getRoomsCount()));
        }
        inventoryRepository.saveAll(inventories);

        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(booking.getPayment());
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Booking request id: {} rejected and inventory released.", bookingId);

        // Publish Kafka event
        kafkaEventProducer.publishBookingEvent(toBookingEvent(saved, BookingEventType.REJECTED));

        return bookingMapper.toBookingResponse(saved);
    }

    private BookingEvent toBookingEvent(Booking booking, BookingEventType type) {
        return BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(type)
                .bookingId(booking.getId())
                .hotelId(booking.getHotel().getId())
                .hotelName(booking.getHotel().getName())
                .roomId(booking.getRoom() != null ? booking.getRoom().getId() : null)
                .guestId(booking.getUser() != null ? booking.getUser().getId() : null)
                .guestName(booking.getUser() != null ? booking.getUser().getName() : null)
                .guestEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)
                .hostId(booking.getHotel().getOwner() != null ? booking.getHotel().getOwner().getId() : null)
                .hostEmail(booking.getHotel().getOwner() != null ? booking.getHotel().getOwner().getEmail() : null)
                .totalPrice(booking.getTotalPrice())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .timestamp(Instant.now())
                .build();
    }

    private void validateHostAccess(Hotel hotel, Long userId) {
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGER");
        boolean isOwner = hotel.getOwner() != null && hotel.getOwner().getId().equals(userId);
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You do not have permission to manage booking requests for this hotel.");
        }
    }

    @Override
    public BookingResponse getBookingById(Long bookingId, Long userId) {
        log.info("Fetching booking id: {} for user id: {}", bookingId, userId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));

        validateBookingAccess(booking, userId);
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    public List<BookingResponse> getMyBookings(Long userId) {
        log.info("Fetching all bookings for user id: {}", userId);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    @Override
    public List<BookingResponse> getBookingsByHotelId(Long hotelId, Long userId) {
        log.info("Fetching bookings for hotel id: {} by user id: {}", hotelId, userId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));

        boolean isAdminOrManager = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGER");
        boolean isOwner = hotel.getOwner() != null && hotel.getOwner().getId().equals(userId);

        if (!isAdminOrManager && !isOwner) {
            throw new ForbiddenException("You do not have permission to view bookings for this hotel.");
        }

        return bookingRepository.findByHotelIdOrderByCreatedAtDesc(hotelId)
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    private void validateBookingDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new BadRequestException("Check-in and check-out dates are required.");
        }
        if (checkInDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Check-in date cannot be in the past.");
        }
        if (!checkInDate.isBefore(checkOutDate)) {
            throw new BadRequestException("Check-in date must be before check-out date.");
        }
    }

    private void validateBookingAccess(Booking booking, Long userId) {
        boolean isOwner = booking.getUser().getId().equals(userId);
        boolean isAdminOrManager = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("MANAGER");
        boolean isHotelOwner = booking.getHotel().getOwner() != null && booking.getHotel().getOwner().getId().equals(userId);

        if (!isOwner && !isAdminOrManager && !isHotelOwner) {
            throw new ForbiddenException("You do not have permission to access this booking.");
        }
    }
}
