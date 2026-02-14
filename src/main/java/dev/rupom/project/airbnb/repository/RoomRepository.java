package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room,Long> {
}
