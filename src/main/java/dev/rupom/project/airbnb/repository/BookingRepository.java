package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByHotelIdOrderByCreatedAtDesc(Long hotelId);
    Optional<Booking> findByIdAndUserId(Long id, Long userId);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime threshold);
}
