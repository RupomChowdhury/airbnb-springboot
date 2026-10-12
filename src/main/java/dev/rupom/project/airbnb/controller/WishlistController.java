package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.WishlistResponse;
import dev.rupom.project.airbnb.service.WishlistService;
import dev.rupom.project.airbnb.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wishlists")
@RequiredArgsConstructor
@Slf4j
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/toggle/{hotelId}")
    public ResponseEntity<Map<String, Object>> toggleWishlist(@PathVariable Long hotelId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User {} toggling wishlist for hotel {}", currentUserId, hotelId);
        Boolean isAdded = wishlistService.toggleWishlist(currentUserId, hotelId);
        return ResponseEntity.ok(Map.of(
                "hotelId", hotelId,
                "wishlisted", isAdded,
                "message", isAdded ? "Added to wishlist" : "Removed from wishlist"
        ));
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getUserWishlist() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Fetching wishlist for user {}", currentUserId);
        return ResponseEntity.ok(wishlistService.getUserWishlist(currentUserId));
    }
}
