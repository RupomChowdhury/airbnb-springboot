package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.service.ReservationExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryController {

    private final ReservationExpiryService reservationExpiryService;

    @PostMapping("/expire-holds")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> triggerExpireHolds() {
        log.info("Admin triggered manual expiration of stale reservation holds");
        int expiredCount = reservationExpiryService.expireStaleReservations();
        return ResponseEntity.ok(Map.of(
                "expiredCount", expiredCount,
                "message", "Expired and released " + expiredCount + " stale booking holds."
        ));
    }
}
