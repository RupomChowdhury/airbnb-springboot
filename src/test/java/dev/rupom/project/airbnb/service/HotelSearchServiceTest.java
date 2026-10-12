package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.HotelSearchRequest;
import dev.rupom.project.airbnb.dto.HotelSearchResponse;
import dev.rupom.project.airbnb.dto.RatingSummaryResponse;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Inventory;
import dev.rupom.project.airbnb.entity.Room;
import dev.rupom.project.airbnb.mapper.HotelMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.InventoryRepository;
import dev.rupom.project.airbnb.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelSearchServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private HotelMapper hotelMapper;

    @Mock
    private ReviewService reviewService;

    @Mock
    private PricingService pricingService;

    @InjectMocks
    private HotelSearchServiceImpl hotelSearchService;

    private Hotel hotel;
    private Room room;
    private Inventory inv1;
    private Inventory inv2;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .id(1L)
                .name("Sea View Resort")
                .city("Cox's Bazar")
                .active(true)
                .build();

        room = Room.builder()
                .id(10L)
                .hotel(hotel)
                .type("Ocean Deluxe")
                .basePrice(BigDecimal.valueOf(150.00))
                .totalCount(5)
                .capacity(2)
                .build();

        LocalDate today = LocalDate.now();
        inv1 = Inventory.builder()
                .id(101L)
                .hotel(hotel)
                .room(room)
                .date(today)
                .totalCount(5)
                .bookedCount(0)
                .surgeFactor(BigDecimal.ONE)
                .price(BigDecimal.valueOf(150.00))
                .closed(false)
                .build();

        inv2 = Inventory.builder()
                .id(102L)
                .hotel(hotel)
                .room(room)
                .date(today.plusDays(1))
                .totalCount(5)
                .bookedCount(1)
                .surgeFactor(BigDecimal.valueOf(1.2))
                .price(BigDecimal.valueOf(180.00))
                .closed(false)
                .build();
    }

    @Test
    void searchHotels_returnsMatchingAvailableHotels() {
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(2);

        HotelSearchRequest request = HotelSearchRequest.builder()
                .city("Cox's Bazar")
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .roomsCount(1)
                .guestsCount(2)
                .build();

        when(hotelRepository.findByCityIgnoreCaseAndActiveTrue("Cox's Bazar")).thenReturn(List.of(hotel));
        when(roomRepository.findAllByHotelId(1L)).thenReturn(List.of(room));
        when(inventoryRepository.findByRoomIdAndDateRange(10L, checkIn, checkOut)).thenReturn(List.of(inv1, inv2));
        when(reviewService.getHotelRatingSummary(1L)).thenReturn(RatingSummaryResponse.builder()
                .averageOverallRating(4.8)
                .totalReviews(10L)
                .isGuestFavorite(true)
                .build());

        List<HotelSearchResponse> results = hotelSearchService.searchHotels(request);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Sea View Resort", results.get(0).getName());
        assertEquals(1, results.get(0).getAvailableRooms().size());

        // Total price = 150 + 180 = 330.00
        assertEquals(0, BigDecimal.valueOf(330.00).compareTo(results.get(0).getMinTotalPrice()));
    }

    @Test
    void searchHotels_roomClosed_notIncluded() {
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(2);

        inv1.setClosed(true);

        HotelSearchRequest request = HotelSearchRequest.builder()
                .city("Cox's Bazar")
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .roomsCount(1)
                .guestsCount(2)
                .build();

        when(hotelRepository.findByCityIgnoreCaseAndActiveTrue("Cox's Bazar")).thenReturn(List.of(hotel));
        when(roomRepository.findAllByHotelId(1L)).thenReturn(List.of(room));
        when(inventoryRepository.findByRoomIdAndDateRange(10L, checkIn, checkOut)).thenReturn(List.of(inv1, inv2));

        List<HotelSearchResponse> results = hotelSearchService.searchHotels(request);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
