package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.RatingSummaryResponse;
import dev.rupom.project.airbnb.dto.ReviewRequest;
import dev.rupom.project.airbnb.dto.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse addReview(Long userId, ReviewRequest request);
    List<ReviewResponse> getHotelReviews(Long hotelId, int page, int size);
    RatingSummaryResponse getHotelRatingSummary(Long hotelId);
}
