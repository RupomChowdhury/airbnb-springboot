package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.InventoryGenerationRequest;
import dev.rupom.project.airbnb.dto.InventoryResponse;
import dev.rupom.project.airbnb.dto.InventoryStatusRequest;
import dev.rupom.project.airbnb.dto.InventorySurgeRequest;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Inventory;
import dev.rupom.project.airbnb.entity.Room;
import dev.rupom.project.airbnb.exception.BadRequestException;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.InventoryMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.InventoryRepository;
import dev.rupom.project.airbnb.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final InventoryMapper mapper;

    private static final String HOTEL_NOT_FOUND = "Hotel not found.";
    private static final String ROOM_NOT_FOUND = "Room not found.";

    @Transactional
    @Override
    public void initializeRoomInventory(Room room, LocalDate startDate, LocalDate endDate) {
        log.info("Initializing inventory for room id: {} between {} and {}", room.getId(), startDate, endDate);
        List<Inventory> inventories = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (!inventoryRepository.existsByHotelIdAndRoomIdAndDate(room.getHotel().getId(), room.getId(), currentDate)) {
                Inventory inventory = Inventory.builder()
                        .hotel(room.getHotel())
                        .room(room)
                        .date(currentDate)
                        .bookedCount(0)
                        .totalCount(room.getTotalCount())
                        .surgeFactor(BigDecimal.ONE)
                        .price(room.getBasePrice()) // basePrice * 1.00
                        .city(room.getHotel().getCity())
                        .closed(false)
                        .build();
                inventories.add(inventory);
            }
            currentDate = currentDate.plusDays(1);
        }

        if (!inventories.isEmpty()) {
            inventoryRepository.saveAll(inventories);
            log.info("Initialized {} inventory records for room id: {}", inventories.size(), room.getId());
        }
    }

    @Transactional
    @Override
    public void initializeHotelInventory(Hotel hotel, LocalDate startDate, LocalDate endDate) {
        log.info("Initializing inventory for all rooms of hotel id: {}", hotel.getId());
        List<Room> rooms = roomRepository.findAllByHotelId(hotel.getId());
        for (Room room : rooms) {
            initializeRoomInventory(room, startDate, endDate);
        }
        log.info("Finished initializing inventories for hotel id: {}", hotel.getId());
    }

    @Transactional
    @Override
    public void deleteFutureInventoriesForHotel(Long hotelId) {
        log.info("Deleting future inventories for hotel id: {}", hotelId);
        inventoryRepository.deleteByHotelIdAndDateGreaterThanEqual(hotelId, LocalDate.now());
        log.info("Deleted future inventories for hotel id: {}", hotelId);
    }

    @Transactional
    @Override
    public void deleteFutureInventoriesForRoom(Long roomId) {
        log.info("Deleting future inventories for room id: {}", roomId);
        inventoryRepository.deleteByRoomIdAndDateGreaterThanEqual(roomId, LocalDate.now());
        log.info("Deleted future inventories for room id: {}", roomId);
    }

    @Override
    public List<InventoryResponse> getInventoriesByRoomAndDateRange(Long hotelId, Long roomId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching inventories for hotel id: {}, room id: {} from {} to {}", hotelId, roomId, startDate, endDate);
        validateHotelAndRoom(hotelId, roomId);

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        return inventoryRepository.findByRoomIdAndDateBetween(roomId, startDate, endDate)
                .stream()
                .map(mapper::toInventoryResponse)
                .toList();
    }

    @Transactional
    @Override
    public Boolean updateSurgeFactor(Long hotelId, Long roomId, InventorySurgeRequest request) {
        log.info("Updating surge factor to {} for room id: {} from {} to {}", request.getSurgeFactor(), roomId, request.getStartDate(), request.getEndDate());
        Room room = validateHotelAndRoom(hotelId, roomId);

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        List<Inventory> inventories = inventoryRepository.findByRoomIdAndDateBetween(
                roomId, request.getStartDate(), request.getEndDate()
        );

        for (Inventory inventory : inventories) {
            inventory.setSurgeFactor(request.getSurgeFactor());
            // Recalculate price: basePrice * surgeFactor
            BigDecimal dynamicPrice = room.getBasePrice().multiply(request.getSurgeFactor());
            inventory.setPrice(dynamicPrice);
        }

        inventoryRepository.saveAll(inventories);
        log.info("Updated surge factor for {} inventory days", inventories.size());
        return true;
    }

    @Transactional
    @Override
    public Boolean updateInventoryStatus(Long hotelId, Long roomId, InventoryStatusRequest request) {
        log.info("Updating closed status to {} for room id: {} from {} to {}", request.getClosed(), roomId, request.getStartDate(), request.getEndDate());
        validateHotelAndRoom(hotelId, roomId);

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        List<Inventory> inventories = inventoryRepository.findByRoomIdAndDateBetween(
                roomId, request.getStartDate(), request.getEndDate()
        );

        for (Inventory inventory : inventories) {
            inventory.setClosed(request.getClosed());
        }

        inventoryRepository.saveAll(inventories);
        log.info("Updated status for {} inventory records", inventories.size());
        return true;
    }

    @Transactional
    @Override
    public Boolean generateCustomInventory(Long hotelId, Long roomId, InventoryGenerationRequest request) {
        log.info("Generating custom inventory for room id: {} between {} and {}", roomId, request.getStartDate(), request.getEndDate());
        Room room = validateHotelAndRoom(hotelId, roomId);

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        initializeRoomInventory(room, request.getStartDate(), request.getEndDate());
        return true;
    }

    private Room validateHotelAndRoom(Long hotelId, Long roomId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException(HOTEL_NOT_FOUND);
        }
        Room room = roomRepository.findByHotelIdAndId(hotelId, roomId);
        if (room == null) {
            throw new ResourceNotFoundException(ROOM_NOT_FOUND);
        }
        return room;
    }
}
