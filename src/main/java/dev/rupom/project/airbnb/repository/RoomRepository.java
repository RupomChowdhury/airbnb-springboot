package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Room findByHotelIdAndId(Long hotelId, Long roomId);
    Optional<Room> findRoomByHotelIdAndId(Long hotelId, Long roomId);
    Boolean existsByHotelIdAndId(Long hotelId, Long roomId);
    void deleteByHotelIdAndId(Long hotelId, Long roomId);
    List<Room> findAllByHotelId(Long hotelId);
    void deleteByHotelId(Long hotelId);
}
