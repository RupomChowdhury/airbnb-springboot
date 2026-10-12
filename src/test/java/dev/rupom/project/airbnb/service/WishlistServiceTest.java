package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.WishlistResponse;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.entity.Wishlist;
import dev.rupom.project.airbnb.mapper.WishlistMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.UserRepository;
import dev.rupom.project.airbnb.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private WishlistMapper wishlistMapper;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User user;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Rupom").build();
        hotel = Hotel.builder().id(10L).name("Luxury Penthouse").build();
    }

    @Test
    void toggleWishlist_notFavorited_addsFavorite() {
        when(wishlistRepository.findByUserIdAndHotelId(1L, 10L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(hotelRepository.findById(10L)).thenReturn(Optional.of(hotel));

        Boolean result = wishlistService.toggleWishlist(1L, 10L);

        assertTrue(result);
        verify(wishlistRepository, times(1)).save(any(Wishlist.class));
        verify(wishlistRepository, never()).delete(any(Wishlist.class));
    }

    @Test
    void toggleWishlist_alreadyFavorited_removesFavorite() {
        Wishlist existing = Wishlist.builder().id(100L).user(user).hotel(hotel).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(hotelRepository.findById(10L)).thenReturn(Optional.of(hotel));
        when(wishlistRepository.findByUserIdAndHotelId(1L, 10L)).thenReturn(Optional.of(existing));

        Boolean result = wishlistService.toggleWishlist(1L, 10L);

        assertFalse(result);
        verify(wishlistRepository, times(1)).delete(existing);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    void getUserWishlist_returnsUserWishlists() {
        Wishlist wishlist = Wishlist.builder().id(100L).user(user).hotel(hotel).build();
        WishlistResponse response = WishlistResponse.builder()
                .id(100L)
                .hotelId(10L)
                .hotelName("Luxury Penthouse")
                .build();

        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(wishlist));
        when(wishlistMapper.toWishlistResponse(wishlist)).thenReturn(response);

        List<WishlistResponse> list = wishlistService.getUserWishlist(1L);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).getHotelId());
    }
}
