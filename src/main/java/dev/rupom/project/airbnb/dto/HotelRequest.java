package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.ContactInfo;
import dev.rupom.project.airbnb.entity.enums.CancellationPolicy;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotelRequest {
    @NotBlank
    private String name;
    @NotBlank
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
    private Boolean instantBookable;
}
