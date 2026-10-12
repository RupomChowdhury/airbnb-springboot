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
public class BookingEvent {
    private String eventId;
    private BookingEventType eventType;
    private Long bookingId;
    private Long hotelId;
    private String hotelName;
    private Long roomId;
    private Long guestId;
    private String guestName;
    private String guestEmail;
    private Long hostId;
    private String hostEmail;
    private BigDecimal totalPrice;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Instant timestamp;
}
