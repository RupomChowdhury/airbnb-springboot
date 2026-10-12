package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.InventoryStatusRequest;
import dev.rupom.project.airbnb.dto.InventorySurgeRequest;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Inventory;
import dev.rupom.project.airbnb.entity.Room;
import dev.rupom.project.airbnb.mapper.InventoryMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.InventoryRepository;
import dev.rupom.project.airbnb.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private InventoryMapper mapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Hotel hotel;
    private Room room;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .id(1L)
                .name("Grand Palace")
                .city("Dhaka")
                .active(true)
                .build();

        room = Room.builder()
                .id(10L)
                .hotel(hotel)
                .type("Deluxe Suite")
                .basePrice(BigDecimal.valueOf(100.00))
                .totalCount(5)
                .capacity(2)
                .build();
    }

    @Test
    void initializeRoomInventory_createsDailyInventories() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(2); // 3 days total

        when(inventoryRepository.existsByHotelIdAndRoomIdAndDate(anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(false);

        inventoryService.initializeRoomInventory(room, startDate, endDate);

        ArgumentCaptor<List<Inventory>> captor = ArgumentCaptor.forClass(List.class);
        verify(inventoryRepository, times(1)).saveAll(captor.capture());

        List<Inventory> saved = captor.getValue();
        assertEquals(3, saved.size());
        assertEquals(BigDecimal.valueOf(100.00), saved.get(0).getPrice());
        assertEquals(BigDecimal.ONE, saved.get(0).getSurgeFactor());
        assertEquals(5, saved.get(0).getTotalCount());
        assertEquals(0, saved.get(0).getBookedCount());
    }

    @Test
    void updateSurgeFactor_recalculatesPrice() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);

        Inventory inventory = Inventory.builder()
                .id(100L)
                .hotel(hotel)
                .room(room)
                .date(startDate)
                .surgeFactor(BigDecimal.ONE)
                .price(BigDecimal.valueOf(100.00))
                .bookedCount(0)
                .totalCount(5)
                .closed(false)
                .build();

        when(hotelRepository.existsById(1L)).thenReturn(true);
        when(roomRepository.findByHotelIdAndId(1L, 10L)).thenReturn(room);
        when(inventoryRepository.findByRoomIdAndDateBetween(10L, startDate, endDate)).thenReturn(List.of(inventory));

        InventorySurgeRequest request = InventorySurgeRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .surgeFactor(BigDecimal.valueOf(1.5))
                .build();

        Boolean result = inventoryService.updateSurgeFactor(1L, 10L, request);

        assertTrue(result);
        assertEquals(BigDecimal.valueOf(1.5), inventory.getSurgeFactor());
        assertEquals(0, BigDecimal.valueOf(150.00).compareTo(inventory.getPrice()));
        verify(inventoryRepository, times(1)).saveAll(any());
    }

    @Test
    void updateInventoryStatus_closesInventory() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);

        Inventory inventory = Inventory.builder()
                .id(100L)
                .hotel(hotel)
                .room(room)
                .date(startDate)
                .closed(false)
                .build();

        when(hotelRepository.existsById(1L)).thenReturn(true);
        when(roomRepository.findByHotelIdAndId(1L, 10L)).thenReturn(room);
        when(inventoryRepository.findByRoomIdAndDateBetween(10L, startDate, endDate)).thenReturn(List.of(inventory));

        InventoryStatusRequest request = InventoryStatusRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .closed(true)
                .build();

        Boolean result = inventoryService.updateInventoryStatus(1L, 10L, request);

        assertTrue(result);
        assertTrue(inventory.getClosed());
        verify(inventoryRepository, times(1)).saveAll(any());
    }

    @Test
    void deleteFutureInventoriesForHotel_callsRepo() {
        inventoryService.deleteFutureInventoriesForHotel(1L);
        verify(inventoryRepository, times(1)).deleteByHotelIdAndDateGreaterThanEqual(eq(1L), any(LocalDate.class));
    }
}
