package dev.rupom.project.airbnb.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyPriceDto {
    private LocalDate date;
    private BigDecimal price;
    private BigDecimal surgeFactor;
}
