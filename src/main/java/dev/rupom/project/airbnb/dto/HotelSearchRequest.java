package dev.rupom.project.airbnb.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSearchRequest {
    private String city;

    private Double latitude;
    private Double longitude;
    private Double radiusKm;

    private Double minLat;
    private Double maxLat;
    private Double minLng;
    private Double maxLng;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOutDate;

    @NotNull(message = "Rooms count is required")
    @Min(value = 1, message = "At least 1 room is required")
    private Integer roomsCount;

    @Min(value = 1, message = "Guests count must be at least 1")
    private Integer guestsCount;
}
