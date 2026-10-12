package dev.rupom.project.airbnb.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomPriceResponse {
    private Long roomId;
    private String roomType;
    private BigDecimal basePrice;
    private BigDecimal avgPricePerNight;
    private BigDecimal totalPrice;
    private Integer capacity;
    private Integer availableCount;
    private String[] photos;
    private String[] amenities;
}
