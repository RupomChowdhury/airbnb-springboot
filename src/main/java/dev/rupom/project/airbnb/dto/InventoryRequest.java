package dev.rupom.project.airbnb.dto;

import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Room;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryRequest {
    private Hotel hotel;
    private Room room;
    private LocalDate date;
    private Integer bookedCount;
    private Integer totalCount;
    private BigDecimal surgeFactor;
    private BigDecimal price;
    private String city;
    private Boolean closed;
}
