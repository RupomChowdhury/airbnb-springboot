package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Payout;
import dev.rupom.project.airbnb.entity.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    List<Payout> findByStatusAndReleaseDateLessThanEqual(PayoutStatus status, LocalDate date);

    List<Payout> findByHostIdOrderByCreatedAtDesc(Long hostId);

    Optional<Payout> findByBookingId(Long bookingId);
}
