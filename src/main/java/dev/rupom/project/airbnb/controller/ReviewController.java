package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.RatingSummaryResponse;
import dev.rupom.project.airbnb.dto.ReviewRequest;
import dev.rupom.project.airbnb.dto.ReviewResponse;
import dev.rupom.project.airbnb.service.ReviewService;
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
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> addReview(@RequestBody @Valid ReviewRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} submitting review for booking {}", currentUserId, request.getBookingId());
        ReviewResponse response = reviewService.addReview(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/reviews/hotel/{hotelId}", "/hotels/{hotelId}/reviews"})
    public ResponseEntity<List<ReviewResponse>> getHotelReviews(
            @PathVariable Long hotelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching reviews for hotel {}, page {}, size {}", hotelId, page, size);
        return ResponseEntity.ok(reviewService.getHotelReviews(hotelId, page, size));
    }

    @GetMapping({"/reviews/hotel/{hotelId}/summary", "/hotels/{hotelId}/rating-summary"})
    public ResponseEntity<RatingSummaryResponse> getHotelRatingSummary(@PathVariable Long hotelId) {
        log.info("Fetching rating summary for hotel {}", hotelId);
        return ResponseEntity.ok(reviewService.getHotelRatingSummary(hotelId));
    }
}
