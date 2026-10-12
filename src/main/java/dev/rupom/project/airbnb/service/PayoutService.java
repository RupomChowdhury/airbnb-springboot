package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.PayoutResponse;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Payout;

import java.util.List;

public interface PayoutService {

    Payout createEscrowPayout(Booking booking);

    void cancelEscrowPayout(Long bookingId);

    void updatePayoutAmount(Long bookingId, java.math.BigDecimal newAmount);

    int releaseDuePayouts();

    PayoutResponse releasePayoutManually(Long payoutId);

    List<PayoutResponse> getPayoutsByHost(Long hostId);

    List<PayoutResponse> getAllPayouts();
}
