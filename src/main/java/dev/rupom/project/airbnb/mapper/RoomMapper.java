package dev.rupom.project.airbnb.mapper;

import dev.rupom.project.airbnb.dto.RoomRequest;
import dev.rupom.project.airbnb.dto.RoomResponse;
import dev.rupom.project.airbnb.entity.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    RoomResponse toRoomResponse(Room response);
    Room toRoom(RoomRequest request);
}
