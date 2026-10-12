package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.CancellationRefundResponse;
import dev.rupom.project.airbnb.dto.PriceBreakdownDto;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PricingService {
    PriceBreakdownDto calculatePriceBreakdown(Hotel hotel, BigDecimal nightlyTotal, long numberOfNights);

    CancellationRefundResponse calculateCancellationRefund(Booking booking, LocalDate cancellationDate);
}
