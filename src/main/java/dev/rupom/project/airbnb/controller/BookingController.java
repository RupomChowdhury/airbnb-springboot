package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.BookingRequest;
import dev.rupom.project.airbnb.dto.BookingResponse;
import dev.rupom.project.airbnb.service.BookingService;
import dev.rupom.project.airbnb.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(@RequestBody @Valid BookingRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Attempting to initiate booking for user: {}", currentUserId);
        BookingResponse response = bookingService.initiateBooking(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Attempting to get booking with id: {} for user: {}", id, currentUserId);
        return ResponseEntity.ok(bookingService.getBookingById(id, currentUserId));
    }

    @GetMapping("/bookings/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Attempting to get all bookings for user: {}", currentUserId);
        return ResponseEntity.ok(bookingService.getMyBookings(currentUserId));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Attempting to cancel booking id: {} by user: {}", id, currentUserId);
        return ResponseEntity.ok(bookingService.cancelBooking(id, currentUserId));
    }

    @GetMapping("/bookings/{id}/cancellation-quote")
    public ResponseEntity<dev.rupom.project.airbnb.dto.CancellationRefundResponse> getCancellationQuote(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Attempting to get cancellation quote for booking id: {} by user: {}", id, currentUserId);
        return ResponseEntity.ok(bookingService.getCancellationQuote(id, currentUserId));
    }

    @PostMapping("/admin/hotels/{hotelId}/bookings/{bookingId}/accept")
    public ResponseEntity<BookingResponse> acceptBookingRequest(@PathVariable Long hotelId, @PathVariable Long bookingId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Host {} attempting to accept booking request id: {} for hotel id: {}", currentUserId, bookingId, hotelId);
        return ResponseEntity.ok(bookingService.acceptBookingRequest(hotelId, bookingId, currentUserId));
    }

    @PostMapping("/admin/hotels/{hotelId}/bookings/{bookingId}/reject")
    public ResponseEntity<BookingResponse> rejectBookingRequest(@PathVariable Long hotelId, @PathVariable Long bookingId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Host {} attempting to reject booking request id: {} for hotel id: {}", currentUserId, bookingId, hotelId);
        return ResponseEntity.ok(bookingService.rejectBookingRequest(hotelId, bookingId, currentUserId));
    }

    @GetMapping("/admin/hotels/{hotelId}/bookings")
    public ResponseEntity<List<BookingResponse>> getBookingsByHotelId(@PathVariable Long hotelId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Attempting to get all bookings for hotel id: {} by user: {}", hotelId, currentUserId);
        return ResponseEntity.ok(bookingService.getBookingsByHotelId(hotelId, currentUserId));
    }
}
