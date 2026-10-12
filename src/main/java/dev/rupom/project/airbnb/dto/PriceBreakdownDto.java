package dev.rupom.project.airbnb.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceBreakdownDto {
    private BigDecimal nightlyTotal;
    private BigDecimal cleaningFee;
    private BigDecimal serviceFee;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalPrice;
    private BigDecimal hostPayout;
}
