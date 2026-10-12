package dev.rupom.project.airbnb.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long hotelId;
    private Long bookingId;
    private Integer overallRating;
    private Integer cleanlinessRating;
    private Integer accuracyRating;
    private Integer communicationRating;
    private Integer locationRating;
    private Integer checkInRating;
    private Integer valueRating;
    private String comment;
    private LocalDateTime createdAt;
}
