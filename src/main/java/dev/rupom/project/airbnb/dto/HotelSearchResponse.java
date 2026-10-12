package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.ContactInfo;
import dev.rupom.project.airbnb.entity.enums.CancellationPolicy;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSearchResponse {
    private Long id;
    private String name;
    private String city;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private String[] photos;
    private String[] amenities;
    private ContactInfo contactInfo;
    private Double averageRating;
    private Long totalReviews;
    private Boolean isGuestFavorite;
    private BigDecimal cleaningFee;
    private CancellationPolicy cancellationPolicy;
    private Boolean instantBookable;
    private BigDecimal minPricePerNight;
    private BigDecimal minTotalPrice;
    private List<RoomPriceResponse> availableRooms;
}
