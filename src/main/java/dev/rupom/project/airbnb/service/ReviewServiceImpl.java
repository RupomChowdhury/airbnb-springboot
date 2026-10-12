package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.config.RedisCacheConfig;
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
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.ReviewMapper;
import dev.rupom.project.airbnb.repository.BookingRepository;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.ReviewRepository;
import dev.rupom.project.airbnb.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    private static final String HOTEL_NOT_FOUND = "Hotel not found.";
    private static final String USER_NOT_FOUND = "User not found.";
    private static final String BOOKING_NOT_FOUND = "Booking not found.";

    @CacheEvict(value = RedisCacheConfig.HOTEL_RATINGS_CACHE, key = "#result.hotelId")
    @Transactional
    @Override
    public ReviewResponse addReview(Long userId, ReviewRequest request) {
        log.info("Adding review for booking id: {} by user id: {}", request.getBookingId(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(BOOKING_NOT_FOUND));

        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only review bookings made by your account.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("You can only review confirmed bookings.");
        }

        if (reviewRepository.existsByBookingIdAndUserId(request.getBookingId(), userId)) {
            throw new ConflictException("You have already submitted a review for this booking.");
        }

        Hotel hotel = booking.getHotel();

        Review review = Review.builder()
                .user(user)
                .hotel(hotel)
                .booking(booking)
                .overallRating(request.getOverallRating())
                .cleanlinessRating(request.getCleanlinessRating())
                .accuracyRating(request.getAccuracyRating())
                .communicationRating(request.getCommunicationRating())
                .locationRating(request.getLocationRating())
                .checkInRating(request.getCheckInRating())
                .valueRating(request.getValueRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Review saved successfully with id: {}", savedReview.getId());

        return reviewMapper.toReviewResponse(savedReview);
    }

    @Override
    public List<ReviewResponse> getHotelReviews(Long hotelId, int page, int size) {
        log.info("Fetching reviews for hotel id: {}, page: {}, size: {}", hotelId, page, size);
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException(HOTEL_NOT_FOUND);
        }

        return reviewRepository.findByHotelIdOrderByCreatedAtDesc(hotelId, PageRequest.of(page, size))
                .stream()
                .map(reviewMapper::toReviewResponse)
                .toList();
    }

    @Cacheable(value = RedisCacheConfig.HOTEL_RATINGS_CACHE, key = "#hotelId")
    @Override
    public RatingSummaryResponse getHotelRatingSummary(Long hotelId) {
        log.info("Generating rating summary for hotel id: {}", hotelId);
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException(HOTEL_NOT_FOUND);
        }

        List<Object[]> stats = reviewRepository.getRatingStatisticsByHotelId(hotelId);

        if (stats == null || stats.isEmpty()) {
            return RatingSummaryResponse.builder()
                    .hotelId(hotelId)
                    .totalReviews(0L)
                    .averageOverallRating(0.0)
                    .isGuestFavorite(false)
                    .build();
        }

        Object[] row = stats.get(0);
        long count = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        double overall = row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : 0.0;
        double cleanliness = row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0 : 0.0;
        double accuracy = row[3] != null ? Math.round(((Number) row[3]).doubleValue() * 10.0) / 10.0 : 0.0;
        double communication = row[4] != null ? Math.round(((Number) row[4]).doubleValue() * 10.0) / 10.0 : 0.0;
        double location = row[5] != null ? Math.round(((Number) row[5]).doubleValue() * 10.0) / 10.0 : 0.0;
        double checkIn = row[6] != null ? Math.round(((Number) row[6]).doubleValue() * 10.0) / 10.0 : 0.0;
        double value = row[7] != null ? Math.round(((Number) row[7]).doubleValue() * 10.0) / 10.0 : 0.0;

        boolean isGuestFavorite = count >= 5 && overall >= 4.8;

        return RatingSummaryResponse.builder()
                .hotelId(hotelId)
                .totalReviews(count)
                .averageOverallRating(overall)
                .averageCleanlinessRating(cleanliness)
                .averageAccuracyRating(accuracy)
                .averageCommunicationRating(communication)
                .averageLocationRating(location)
                .averageCheckInRating(checkIn)
                .averageValueRating(value)
                .isGuestFavorite(isGuestFavorite)
                .build();
    }
}
