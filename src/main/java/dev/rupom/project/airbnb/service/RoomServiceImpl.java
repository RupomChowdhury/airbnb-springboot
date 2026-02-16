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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService{
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomMapper mapper;
    private static final String HOTEL_NOT_FOUND = "Hotel not found.";

    @Transactional
    @Override
    public RoomResponse createNewRoom(Long hotelId, RoomRequest request) {
        log.info("Creating a new room for hotel with id: {}",hotelId);
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(()-> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        Room room = mapper.toRoom(request);
        room.setHotel(hotel);
        room = roomRepository.save(room);
        hotel.getRooms().add(room);
        hotelRepository.save(hotel);
        log.info("Created a new room for hotel with id: {}",hotelId);

        //TODO: Create inventory for this room.
        return mapper.toRoomResponse(room);
    }

    @Override
    public List<RoomResponse> getAllRoomsByHotelId(Long hotelId) {
        log.info("Getting all rooms for hotel with id: {}",hotelId);
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(()-> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        log.info("Got all rooms for hotel with id: {}",hotelId);
        return hotel.getRooms()
                .stream()
                .map(mapper::toRoomResponse)
                .toList();
    }

    @Override
    public RoomResponse getRoomById(Long hotelId, Long roomId) {
        log.info("Getting room with id: {} for hotel with id: {}",roomId,hotelId);
        isExistByHotelIdAndId(hotelId,roomId);
        Room room = roomRepository.findByHotelIdAndId(hotelId, roomId);
        log.info("Got room with id: {} for hotel with id: {}",roomId,hotelId);
        return mapper.toRoomResponse(room);
    }

    @Transactional
    @Override
    public Boolean deleteRoomById(Long hotelId, Long roomId) {
        log.info("Deleting room with id: {} for hotel with id: {}",roomId,hotelId);
        isExistByHotelIdAndId(hotelId,roomId);
        roomRepository.deleteByHotelIdAndId(hotelId,roomId);
        log.info("Deleted room with id: {} for hotel with id: {}",roomId,hotelId);
        //TODO: delete the future inventories for this room.
        return true;
    }

    void isExistByHotelIdAndId(Long hotelId, Long roomId){
        boolean exists = roomRepository.existsByHotelIdAndId(hotelId,roomId);
        if (!exists){
            throw new ResourceNotFoundException(HOTEL_NOT_FOUND);
        }
    }
}
