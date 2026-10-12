package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.ContactInfo;
import dev.rupom.project.airbnb.entity.enums.CancellationPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotelResponse {
    private Long id;
    private String name;
    private String city;
    private Double latitude;
    private Double longitude;
    private String[] photos;
    private String[] amenities;
    private ContactInfo contactInfo;
    private Boolean active;
    private BigDecimal cleaningFee;
    private BigDecimal weeklyDiscountPercent;
    private CancellationPolicy cancellationPolicy;
    private Double averageRating;
    private Long totalReviews;
    private Boolean isGuestFavorite;
    private Boolean instantBookable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
