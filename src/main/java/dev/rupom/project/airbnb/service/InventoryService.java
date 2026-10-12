package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.InventoryGenerationRequest;
import dev.rupom.project.airbnb.dto.InventoryResponse;
import dev.rupom.project.airbnb.dto.InventoryStatusRequest;
import dev.rupom.project.airbnb.dto.InventorySurgeRequest;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Room;

import java.time.LocalDate;
import java.util.List;

public interface InventoryService {
    void initializeRoomInventory(Room room, LocalDate startDate, LocalDate endDate);
    void initializeHotelInventory(Hotel hotel, LocalDate startDate, LocalDate endDate);
    void deleteFutureInventoriesForHotel(Long hotelId);
    void deleteFutureInventoriesForRoom(Long roomId);
    List<InventoryResponse> getInventoriesByRoomAndDateRange(Long hotelId, Long roomId, LocalDate startDate, LocalDate endDate);
    Boolean updateSurgeFactor(Long hotelId, Long roomId, InventorySurgeRequest request);
    Boolean updateInventoryStatus(Long hotelId, Long roomId, InventoryStatusRequest request);
    Boolean generateCustomInventory(Long hotelId, Long roomId, InventoryGenerationRequest request);
}
