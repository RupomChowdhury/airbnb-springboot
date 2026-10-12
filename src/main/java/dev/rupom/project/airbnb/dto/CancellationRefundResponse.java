package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.enums.CancellationPolicy;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationRefundResponse {
    private Long bookingId;
    private CancellationPolicy cancellationPolicy;
    private BigDecimal totalPaid;
    private BigDecimal refundAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal hostPayoutAmount;
    private String explanation;
}
