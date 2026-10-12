package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.enums.CancellationPolicy;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelPriceQuoteResponse {
    private Long hotelId;
    private String hotelName;
    private Long roomId;
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Long totalNights;
    private Integer roomsCount;
    private BigDecimal totalPrice;
    private BigDecimal avgPricePerNight;
    private PriceBreakdownDto priceBreakdown;
    private CancellationPolicy cancellationPolicy;
    private List<DailyPriceDto> dailyBreakdown;
}
