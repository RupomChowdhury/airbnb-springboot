package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
    private Long hotelId;
    private String hotelName;
    private Long roomId;
    private String roomType;
    private Long userId;
    private String userName;
    private BookingStatus status;
    private Integer roomsCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal nightlyTotal;
    private BigDecimal cleaningFee;
    private BigDecimal serviceFee;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalPrice;
    private BigDecimal hostPayout;
    private Set<GuestResponse> guests;
    private PaymentResponse payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
