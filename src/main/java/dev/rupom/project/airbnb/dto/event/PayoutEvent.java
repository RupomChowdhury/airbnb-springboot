package dev.rupom.project.airbnb.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutEvent {
    private String eventId;
    private PayoutEventType eventType;
    private Long payoutId;
    private Long bookingId;
    private Long hostId;
    private String hostName;
    private String hostEmail;
    private BigDecimal amount;
    private LocalDate releaseDate;
    private Instant timestamp;
}
