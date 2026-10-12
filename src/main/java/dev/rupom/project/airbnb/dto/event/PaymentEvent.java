package dev.rupom.project.airbnb.dto.event;

import dev.rupom.project.airbnb.entity.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String eventId;
    private PaymentEventType eventType;
    private Long paymentId;
    private Long bookingId;
    private String transactionId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String guestEmail;
    private String guestName;
    private Instant timestamp;
}
