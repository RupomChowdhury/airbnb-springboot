package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.WishlistResponse;

import java.util.List;

public interface WishlistService {
    Boolean toggleWishlist(Long userId, Long hotelId);
    List<WishlistResponse> getUserWishlist(Long userId);
}
