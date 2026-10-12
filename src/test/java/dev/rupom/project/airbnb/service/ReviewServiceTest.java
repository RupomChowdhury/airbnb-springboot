package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.RatingSummaryResponse;
import dev.rupom.project.airbnb.dto.ReviewRequest;
import dev.rupom.project.airbnb.dto.ReviewResponse;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Review;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.entity.enums.BookingStatus;
import dev.rupom.project.airbnb.exception.BadRequestException;
import dev.rupom.project.airbnb.exception.ConflictException;
import dev.rupom.project.airbnb.exception.ForbiddenException;
import dev.rupom.project.airbnb.mapper.ReviewMapper;
import dev.rupom.project.airbnb.repository.BookingRepository;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.ReviewRepository;
import dev.rupom.project.airbnb.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User user;
    private Hotel hotel;
    private Booking completedBooking;
    private ReviewRequest reviewRequest;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Rupom").build();
        hotel = Hotel.builder().id(10L).name("Beachfront Resort").build();

        completedBooking = Booking.builder()
                .id(100L)
                .user(user)
                .hotel(hotel)
                .status(BookingStatus.CONFIRMED)
                .checkInDate(LocalDate.now().minusDays(5))
                .checkOutDate(LocalDate.now().minusDays(1)) // stay completed in past
                .build();

        reviewRequest = ReviewRequest.builder()
                .bookingId(100L)
                .overallRating(5)
                .cleanlinessRating(5)
                .accuracyRating(5)
                .communicationRating(5)
                .locationRating(5)
                .checkInRating(5)
                .valueRating(5)
                .comment("Outstanding stay!")
                .build();
    }

    @Test
    void addReview_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(completedBooking));
        when(reviewRepository.existsByBookingIdAndUserId(100L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(reviewMapper.toReviewResponse(any(Review.class))).thenReturn(ReviewResponse.builder()
                .id(1L)
                .hotelId(10L)
                .overallRating(5)
                .comment("Outstanding stay!")
                .build());

        ReviewResponse response = reviewService.addReview(1L, reviewRequest);

        assertNotNull(response);
        assertEquals(5, response.getOverallRating());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void addReview_notBookingOwner_throwsForbiddenException() {
        when(userRepository.findById(999L)).thenReturn(Optional.of(User.builder().id(999L).build()));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(completedBooking));

        // Attempt by user 999 instead of booking owner 1
        assertThrows(ForbiddenException.class, () -> reviewService.addReview(999L, reviewRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_duplicateReview_throwsConflictException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(completedBooking));
        when(reviewRepository.existsByBookingIdAndUserId(100L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> reviewService.addReview(1L, reviewRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_notConfirmedBooking_throwsBadRequestException() {
        completedBooking.setStatus(BookingStatus.RESERVED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(completedBooking));

        assertThrows(BadRequestException.class, () -> reviewService.addReview(1L, reviewRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void getHotelRatingSummary_withReviews_computesGuestFavorite() {
        when(hotelRepository.existsById(10L)).thenReturn(true);
        Object[] row = new Object[]{
                10L, 4.9, 4.9, 5.0, 5.0, 5.0, 4.8, 4.9
        };
        when(reviewRepository.getRatingStatisticsByHotelId(10L)).thenReturn(List.<Object[]>of(row));

        RatingSummaryResponse summary = reviewService.getHotelRatingSummary(10L);

        assertNotNull(summary);
        assertEquals(10L, summary.getTotalReviews());
        assertEquals(4.9, summary.getAverageOverallRating());
        assertTrue(summary.getIsGuestFavorite()); // >= 4.8 and >= 5 reviews
    }
}
