package dev.rupom.project.airbnb.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingSummaryResponse {
    private Long hotelId;
    private Long totalReviews;
    private Double averageOverallRating;
    private Double averageCleanlinessRating;
    private Double averageAccuracyRating;
    private Double averageCommunicationRating;
    private Double averageLocationRating;
    private Double averageCheckInRating;
    private Double averageValueRating;
    private Boolean isGuestFavorite;
}
