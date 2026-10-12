package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByHotelIdAndRoomIdAndDate(Long hotelId, Long roomId, LocalDate date);

    boolean existsByHotelIdAndRoomIdAndDate(Long hotelId, Long roomId, LocalDate date);

    @Query("SELECT i FROM Inventory i WHERE i.room.id = :roomId AND i.date >= :startDate AND i.date <= :endDate ORDER BY i.date ASC")
    List<Inventory> findByRoomIdAndDateBetween(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.room.id = :roomId AND i.date >= :startDate AND i.date < :endDate ORDER BY i.date ASC")
    List<Inventory> findAndLockByRoomIdAndDateRange(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT i FROM Inventory i WHERE i.room.id = :roomId AND i.date >= :startDate AND i.date < :endDate ORDER BY i.date ASC")
    List<Inventory> findByRoomIdAndDateRange(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    void deleteByHotelIdAndDateGreaterThanEqual(Long hotelId, LocalDate date);

    void deleteByRoomIdAndDateGreaterThanEqual(Long roomId, LocalDate date);

    void deleteByHotelId(Long hotelId);

    void deleteByRoomId(Long roomId);
}
