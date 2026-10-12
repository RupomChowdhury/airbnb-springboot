package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.PayoutResponse;
import dev.rupom.project.airbnb.service.PayoutService;
import dev.rupom.project.airbnb.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/payouts")
@RequiredArgsConstructor
@Slf4j
public class PayoutController {

    private final PayoutService payoutService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PayoutResponse>> getAllPayouts() {
        log.info("Admin fetching all escrow payouts");
        return ResponseEntity.ok(payoutService.getAllPayouts());
    }

    @GetMapping("/my-payouts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<PayoutResponse>> getHostPayouts() {
        Long hostId = SecurityUtils.getCurrentUserId();
        log.info("Host {} fetching their escrow payouts", hostId);
        return ResponseEntity.ok(payoutService.getPayoutsByHost(hostId));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PayoutResponse> releasePayoutManually(@PathVariable Long id) {
        log.info("Admin manually releasing payout id: {}", id);
        return ResponseEntity.ok(payoutService.releasePayoutManually(id));
    }

    @PostMapping("/sweep")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerPayoutSweep() {
        log.info("Admin manually triggering escrow payout release sweep");
        int count = payoutService.releaseDuePayouts();
        return ResponseEntity.ok(Map.of(
                "releasedCount", count,
                "message", "Processed and released " + count + " due escrow payouts."
        ));
    }
}
