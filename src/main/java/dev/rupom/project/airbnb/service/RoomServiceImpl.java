package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.RoomRequest;
import dev.rupom.project.airbnb.dto.RoomResponse;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Room;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.RoomMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomMapper mapper;
    private final InventoryService inventoryService;
    private static final String HOTEL_NOT_FOUND = "Hotel not found.";
    private static final String ROOM_NOT_FOUND = "Room not found.";

    @Transactional
    @Override
    public RoomResponse createNewRoom(Long hotelId, RoomRequest request) {
        log.info("Creating a new room for hotel with id: {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        Room room = mapper.toRoom(request);
        room.setHotel(hotel);
        room = roomRepository.save(room);
        hotel.getRooms().add(room);
        hotelRepository.save(hotel);
        log.info("Created a new room for hotel with id: {}", hotelId);

        // Create inventory for this room for the next 365 days
        if (Boolean.TRUE.equals(hotel.getActive())) {
            inventoryService.initializeRoomInventory(room, LocalDate.now(), LocalDate.now().plusDays(365));
        }

        return mapper.toRoomResponse(room);
    }

    @Override
    public List<RoomResponse> getAllRoomsByHotelId(Long hotelId) {
        log.info("Getting all rooms for hotel with id: {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        log.info("Got all rooms for hotel with id: {}", hotelId);
        return hotel.getRooms()
                .stream()
                .map(mapper::toRoomResponse)
                .toList();
    }

    @Override
    public RoomResponse getRoomById(Long hotelId, Long roomId) {
        log.info("Getting room with id: {} for hotel with id: {}", roomId, hotelId);
        isExistByHotelIdAndId(hotelId, roomId);
        Room room = roomRepository.findByHotelIdAndId(hotelId, roomId);
        log.info("Got room with id: {} for hotel with id: {}", roomId, hotelId);
        return mapper.toRoomResponse(room);
    }

    @Transactional
    @Override
    public RoomResponse updateRoomById(Long hotelId, Long roomId, RoomRequest request) {
        log.info("Updating room with id: {} for hotel with id: {}", roomId, hotelId);
        isExistByHotelIdAndId(hotelId, roomId);
        Room room = roomRepository.findByHotelIdAndId(hotelId, roomId);
        mapper.updateRoomFromRequest(request, room);
        Room updatedRoom = roomRepository.save(room);
        log.info("Updated room with id: {} for hotel with id: {}", roomId, hotelId);
        return mapper.toRoomResponse(updatedRoom);
    }

    @Transactional
    @Override
    public Boolean deleteRoomById(Long hotelId, Long roomId) {
        log.info("Deleting room with id: {} for hotel with id: {}", roomId, hotelId);
        isExistByHotelIdAndId(hotelId, roomId);
        // Delete the future inventories for this room
        inventoryService.deleteFutureInventoriesForRoom(roomId);
        roomRepository.deleteByHotelIdAndId(hotelId, roomId);
        log.info("Deleted room with id: {} for hotel with id: {}", roomId, hotelId);
        return true;
    }

    void isExistByHotelIdAndId(Long hotelId, Long roomId) {
        boolean hotelExists = hotelRepository.existsById(hotelId);
        if (!hotelExists) {
            throw new ResourceNotFoundException(HOTEL_NOT_FOUND);
        }
        boolean exists = roomRepository.existsByHotelIdAndId(hotelId, roomId);
        if (!exists) {
            throw new ResourceNotFoundException(ROOM_NOT_FOUND);
        }
    }
}
