package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.PriceBreakdownDto;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Hotel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Slf4j
public class PricingServiceImpl implements PricingService {

    private static final BigDecimal GUEST_SERVICE_FEE_RATE = BigDecimal.valueOf(0.14); // 14% Airbnb guest service fee
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.05); // 5% occupancy tax
    private static final BigDecimal HOST_PAYOUT_RATE = BigDecimal.valueOf(0.97); // 3% host commission deducted

    @Override
    public PriceBreakdownDto calculatePriceBreakdown(Hotel hotel, BigDecimal nightlyTotal, long numberOfNights) {
        log.info("Calculating price breakdown for hotel id: {}, nightly total: {}, nights: {}", hotel.getId(), nightlyTotal, numberOfNights);

        BigDecimal cleaningFee = hotel.getCleaningFee() != null ? hotel.getCleaningFee() : BigDecimal.ZERO;

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (numberOfNights >= 7 && hotel.getWeeklyDiscountPercent() != null && hotel.getWeeklyDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = nightlyTotal.multiply(hotel.getWeeklyDiscountPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal adjustedNightly = nightlyTotal.subtract(discountAmount);
        BigDecimal serviceFee = adjustedNightly.multiply(GUEST_SERVICE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotalBeforeTax = adjustedNightly.add(cleaningFee).add(serviceFee);
        BigDecimal taxAmount = subtotalBeforeTax.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = subtotalBeforeTax.add(taxAmount);

        BigDecimal hostPayout = adjustedNightly.add(cleaningFee).multiply(HOST_PAYOUT_RATE).setScale(2, RoundingMode.HALF_UP);

        log.info("Calculated total price: {} (discount: {}, cleaning: {}, service: {}, tax: {})",
                totalPrice, discountAmount, cleaningFee, serviceFee, taxAmount);

        return PriceBreakdownDto.builder()
                .nightlyTotal(nightlyTotal)
                .cleaningFee(cleaningFee)
                .serviceFee(serviceFee)
                .discountAmount(discountAmount)
                .taxAmount(taxAmount)
                .totalPrice(totalPrice)
                .hostPayout(hostPayout)
                .build();
    }

    @Override
    public dev.rupom.project.airbnb.dto.CancellationRefundResponse calculateCancellationRefund(
            Booking booking, LocalDate cancellationDate) {
        log.info("Calculating tiered cancellation refund for booking id: {} on date: {}", booking.getId(), cancellationDate);

        var policy = booking.getHotel().getCancellationPolicy() != null
                ? booking.getHotel().getCancellationPolicy()
                : dev.rupom.project.airbnb.entity.enums.CancellationPolicy.FLEXIBLE;

        LocalDate checkIn = booking.getCheckInDate();
        long daysUntilCheckIn = java.time.temporal.ChronoUnit.DAYS.between(cancellationDate, checkIn);

        BigDecimal totalPaid = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal cleaningFee = booking.getCleaningFee() != null ? booking.getCleaningFee() : BigDecimal.ZERO;
        BigDecimal accommodation = totalPaid.subtract(cleaningFee).max(BigDecimal.ZERO);
        BigDecimal baseHostPayout = booking.getHostPayout() != null ? booking.getHostPayout() : BigDecimal.ZERO;

        BigDecimal refundAmount;
        BigDecimal hostPayoutAmount;
        BigDecimal penaltyAmount;
        String explanation;

        if (daysUntilCheckIn < 0) {
            // Stay already started
            refundAmount = BigDecimal.ZERO;
            hostPayoutAmount = baseHostPayout;
            penaltyAmount = totalPaid;
            explanation = "Cancellation after check-in date. Non-refundable.";
        } else if (policy == dev.rupom.project.airbnb.entity.enums.CancellationPolicy.FLEXIBLE) {
            if (daysUntilCheckIn >= 1) {
                refundAmount = totalPaid;
                hostPayoutAmount = BigDecimal.ZERO;
                penaltyAmount = BigDecimal.ZERO;
                explanation = "Full 100% refund issued per Flexible policy (cancelled at least 24 hours prior to check-in).";
            } else {
                long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
                nights = nights > 0 ? nights : 1;
                BigDecimal oneNightCost = accommodation.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
                penaltyAmount = oneNightCost;
                refundAmount = totalPaid.subtract(oneNightCost).max(BigDecimal.ZERO);
                hostPayoutAmount = oneNightCost.multiply(BigDecimal.valueOf(0.97)).setScale(2, RoundingMode.HALF_UP);
                explanation = "First night accommodation is non-refundable per Flexible policy (cancelled within 24 hours). Remaining nights and cleaning fee refunded.";
            }
        } else if (policy == dev.rupom.project.airbnb.entity.enums.CancellationPolicy.MODERATE) {
            if (daysUntilCheckIn >= 5) {
                refundAmount = totalPaid;
                hostPayoutAmount = BigDecimal.ZERO;
                penaltyAmount = BigDecimal.ZERO;
                explanation = "Full 100% refund issued per Moderate policy (cancelled at least 5 days prior to check-in).";
            } else {
                BigDecimal nonRefundableAcc = accommodation.multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP);
                refundAmount = totalPaid.subtract(nonRefundableAcc).max(BigDecimal.ZERO);
                penaltyAmount = nonRefundableAcc;
                hostPayoutAmount = nonRefundableAcc.multiply(BigDecimal.valueOf(0.97)).setScale(2, RoundingMode.HALF_UP);
                explanation = "50% accommodation penalty applied per Moderate policy (cancelled less than 5 days prior to check-in). Cleaning fee and 50% accommodation refunded.";
            }
        } else { // STRICT
            if (daysUntilCheckIn >= 7) {
                BigDecimal nonRefundableAcc = accommodation.multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP);
                refundAmount = totalPaid.subtract(nonRefundableAcc).max(BigDecimal.ZERO);
                penaltyAmount = nonRefundableAcc;
                hostPayoutAmount = nonRefundableAcc.multiply(BigDecimal.valueOf(0.97)).setScale(2, RoundingMode.HALF_UP);
                explanation = "50% accommodation refund issued per Strict policy (cancelled at least 7 days prior to check-in).";
            } else {
                refundAmount = cleaningFee;
                penaltyAmount = accommodation;
                hostPayoutAmount = accommodation.multiply(BigDecimal.valueOf(0.97)).setScale(2, RoundingMode.HALF_UP);
                explanation = "Accommodation is completely non-refundable per Strict policy (cancelled less than 7 days prior to check-in). Cleaning fee refunded.";
            }
        }

        log.info("Cancellation outcome for booking id: {} -> Refund: {}, Penalty: {}, HostPayout: {}",
                booking.getId(), refundAmount, penaltyAmount, hostPayoutAmount);

        return dev.rupom.project.airbnb.dto.CancellationRefundResponse.builder()
                .bookingId(booking.getId())
                .cancellationPolicy(policy)
                .totalPaid(totalPaid)
                .refundAmount(refundAmount)
                .penaltyAmount(penaltyAmount)
                .hostPayoutAmount(hostPayoutAmount)
                .explanation(explanation)
                .build();
    }
}
