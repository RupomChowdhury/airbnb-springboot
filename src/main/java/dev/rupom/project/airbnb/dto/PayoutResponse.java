package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.enums.PayoutStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutResponse {
    private Long id;
    private Long bookingId;
    private Long hostId;
    private String hostName;
    private BigDecimal amount;
    private PayoutStatus status;
    private LocalDate releaseDate;
    private LocalDateTime releasedAt;
    private LocalDateTime createdAt;
}
