package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.WishlistResponse;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.entity.Wishlist;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.WishlistMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.UserRepository;
import dev.rupom.project.airbnb.repository.WishlistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final WishlistMapper wishlistMapper;

    private static final String HOTEL_NOT_FOUND = "Hotel not found.";
    private static final String USER_NOT_FOUND = "User not found.";

    @Transactional
    @Override
    public Boolean toggleWishlist(Long userId, Long hotelId) {
        log.info("Toggling wishlist for user id: {}, hotel id: {}", userId, hotelId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));

        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndHotelId(userId, hotelId);

        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            log.info("Removed hotel id: {} from wishlist for user id: {}", hotelId, userId);
            return false; // Removed from wishlist
        } else {
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .hotel(hotel)
                    .build();
            wishlistRepository.save(wishlist);
            log.info("Added hotel id: {} to wishlist for user id: {}", hotelId, userId);
            return true; // Added to wishlist
        }
    }

    @Override
    public List<WishlistResponse> getUserWishlist(Long userId) {
        log.info("Fetching wishlist for user id: {}", userId);
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(wishlistMapper::toWishlistResponse)
                .toList();
    }
}
