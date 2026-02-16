package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.RoomRequest;
import dev.rupom.project.airbnb.dto.RoomResponse;

import java.util.List;

public interface RoomService {
    RoomResponse createNewRoom(Long hotelId, RoomRequest request);
    List<RoomResponse> getAllRoomsByHotelId(Long hotelId);
    RoomResponse getRoomById(Long hotelId, Long roomId);
    Boolean deleteRoomById(Long hotelId, Long roomId);
}
