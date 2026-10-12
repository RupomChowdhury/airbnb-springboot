package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.enums.PaymentMethod;
import dev.rupom.project.airbnb.entity.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private String transactionId;
    private BigDecimal price;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private Long bookingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
