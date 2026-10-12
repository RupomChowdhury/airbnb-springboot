package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.CancellationRefundResponse;
import dev.rupom.project.airbnb.dto.PriceBreakdownDto;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.enums.CancellationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingServiceImpl();
    }

    @Test
    void calculatePriceBreakdown_shortStay_noDiscount() {
        Hotel hotel = Hotel.builder()
                .id(1L)
                .name("Grand Hotel")
                .cleaningFee(BigDecimal.valueOf(50.00))
                .weeklyDiscountPercent(BigDecimal.valueOf(10.0))
                .build();

        // 3 nights, nightlyTotal = $300.00
        PriceBreakdownDto breakdown = pricingService.calculatePriceBreakdown(hotel, BigDecimal.valueOf(300.00), 3);

        assertNotNull(breakdown);
        assertEquals(0, BigDecimal.valueOf(300.00).compareTo(breakdown.getNightlyTotal()));
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(breakdown.getCleaningFee()));
        assertEquals(0, BigDecimal.ZERO.compareTo(breakdown.getDiscountAmount())); // < 7 nights

        // Subtotal accommodation = 300.00
        // Service fee = 14% of 300 = 42.00
        assertEquals(0, BigDecimal.valueOf(42.00).compareTo(breakdown.getServiceFee()));

        // Subtotal before tax = 300 + 50 + 42 = 392.00
        // Tax = 5% of 392 = 19.60
        assertEquals(0, BigDecimal.valueOf(19.60).compareTo(breakdown.getTaxAmount()));

        // Total price = 392 + 19.60 = 411.60
        assertEquals(0, BigDecimal.valueOf(411.60).compareTo(breakdown.getTotalPrice()));

        // Host earnings = (300 + 50) * 0.97 = 339.50
        assertEquals(0, BigDecimal.valueOf(339.50).compareTo(breakdown.getHostPayout()));
    }

    @Test
    void calculatePriceBreakdown_weeklyStay_appliesDiscount() {
        Hotel hotel = Hotel.builder()
                .id(1L)
                .name("Seaside Villa")
                .cleaningFee(BigDecimal.valueOf(100.00))
                .weeklyDiscountPercent(BigDecimal.valueOf(10.0)) // 10%
                .build();

        // 7 nights, nightlyTotal = $700.00
        PriceBreakdownDto breakdown = pricingService.calculatePriceBreakdown(hotel, BigDecimal.valueOf(700.00), 7);

        assertNotNull(breakdown);
        assertEquals(0, BigDecimal.valueOf(700.00).compareTo(breakdown.getNightlyTotal()));
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(breakdown.getCleaningFee()));
        // 10% of 700 = 70.00 discount
        assertEquals(0, BigDecimal.valueOf(70.00).compareTo(breakdown.getDiscountAmount()));

        // Adjusted accommodation = 700 - 70 = 630.00
        // Service fee = 14% of 630 = 88.20
        assertEquals(0, BigDecimal.valueOf(88.20).compareTo(breakdown.getServiceFee()));

        // Subtotal before tax = 630 + 100 + 88.20 = 818.20
        // Tax = 5% of 818.20 = 40.91
        assertEquals(0, BigDecimal.valueOf(40.91).compareTo(breakdown.getTaxAmount()));

        // Total price = 818.20 + 40.91 = 859.11
        assertEquals(0, BigDecimal.valueOf(859.11).compareTo(breakdown.getTotalPrice()));

        // Host earnings = (630 + 100) * 0.97 = 708.10
        assertEquals(0, BigDecimal.valueOf(708.10).compareTo(breakdown.getHostPayout()));
    }

    @Test
    void calculateCancellationRefund_flexible_moreThan24Hours_fullRefund() {
        Hotel hotel = Hotel.builder().cancellationPolicy(CancellationPolicy.FLEXIBLE).build();
        Booking booking = Booking.builder()
                .id(10L)
                .hotel(hotel)
                .checkInDate(LocalDate.now().plusDays(3))
                .checkOutDate(LocalDate.now().plusDays(5))
                .totalPrice(BigDecimal.valueOf(400.00))
                .cleaningFee(BigDecimal.valueOf(50.00))
                .hostPayout(BigDecimal.valueOf(339.50))
                .build();

        CancellationRefundResponse refund = pricingService.calculateCancellationRefund(booking, LocalDate.now());

        assertNotNull(refund);
        assertEquals(0, BigDecimal.valueOf(400.00).compareTo(refund.getRefundAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(refund.getPenaltyAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(refund.getHostPayoutAmount()));
    }

    @Test
    void calculateCancellationRefund_moderate_lessThan5Days_partialPenalty() {
        Hotel hotel = Hotel.builder().cancellationPolicy(CancellationPolicy.MODERATE).build();
        Booking booking = Booking.builder()
                .id(11L)
                .hotel(hotel)
                .checkInDate(LocalDate.now().plusDays(3)) // 3 days < 5 days
                .checkOutDate(LocalDate.now().plusDays(5))
                .totalPrice(BigDecimal.valueOf(500.00))
                .cleaningFee(BigDecimal.valueOf(100.00))
                .hostPayout(BigDecimal.valueOf(388.00))
                .build();

        CancellationRefundResponse refund = pricingService.calculateCancellationRefund(booking, LocalDate.now());

        assertNotNull(refund);
        // Accommodation = 500 - 100 = 400. 50% non-refundable = 200.
        // Refund = 500 - 200 = 300.
        assertEquals(0, BigDecimal.valueOf(300.00).compareTo(refund.getRefundAmount()));
        assertEquals(0, BigDecimal.valueOf(200.00).compareTo(refund.getPenaltyAmount()));
        // Host payout = 200 * 0.97 = 194.00
        assertEquals(0, BigDecimal.valueOf(194.00).compareTo(refund.getHostPayoutAmount()));
    }

    @Test
    void calculateCancellationRefund_strict_lessThan7Days_onlyCleaningFeeRefunded() {
        Hotel hotel = Hotel.builder().cancellationPolicy(CancellationPolicy.STRICT).build();
        Booking booking = Booking.builder()
                .id(12L)
                .hotel(hotel)
                .checkInDate(LocalDate.now().plusDays(2)) // 2 days < 7 days
                .checkOutDate(LocalDate.now().plusDays(5))
                .totalPrice(BigDecimal.valueOf(500.00))
                .cleaningFee(BigDecimal.valueOf(80.00))
                .hostPayout(BigDecimal.valueOf(407.40))
                .build();

        CancellationRefundResponse refund = pricingService.calculateCancellationRefund(booking, LocalDate.now());

        assertNotNull(refund);
        // Only cleaning fee is refunded: 80.00
        assertEquals(0, BigDecimal.valueOf(80.00).compareTo(refund.getRefundAmount()));
        // Accommodation penalty: 500 - 80 = 420.00
        assertEquals(0, BigDecimal.valueOf(420.00).compareTo(refund.getPenaltyAmount()));
        // Host payout = 420 * 0.97 = 407.40
        assertEquals(0, BigDecimal.valueOf(407.40).compareTo(refund.getHostPayoutAmount()));
    }
}
