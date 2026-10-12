package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.BookingRequest;
import dev.rupom.project.airbnb.dto.BookingResponse;
import dev.rupom.project.airbnb.dto.CancellationRefundResponse;

import java.util.List;

public interface BookingService {
    BookingResponse initiateBooking(Long userId, BookingRequest request);
    BookingResponse cancelBooking(Long bookingId, Long userId);
    CancellationRefundResponse getCancellationQuote(Long bookingId, Long userId);
    BookingResponse acceptBookingRequest(Long hotelId, Long bookingId, Long hostUserId);
    BookingResponse rejectBookingRequest(Long hotelId, Long bookingId, Long hostUserId);
    BookingResponse getBookingById(Long bookingId, Long userId);
    List<BookingResponse> getMyBookings(Long userId);
    List<BookingResponse> getBookingsByHotelId(Long hotelId, Long userId);
}
