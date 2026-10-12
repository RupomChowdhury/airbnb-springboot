package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByHotelIdOrderByCreatedAtDesc(Long hotelId, Pageable pageable);

    List<Review> findByHotelId(Long hotelId);

    boolean existsByBookingIdAndUserId(Long bookingId, Long userId);

    @Query("SELECT COUNT(r), " +
            "COALESCE(AVG(r.overallRating), 0.0), " +
            "COALESCE(AVG(r.cleanlinessRating), 0.0), " +
            "COALESCE(AVG(r.accuracyRating), 0.0), " +
            "COALESCE(AVG(r.communicationRating), 0.0), " +
            "COALESCE(AVG(r.locationRating), 0.0), " +
            "COALESCE(AVG(r.checkInRating), 0.0), " +
            "COALESCE(AVG(r.valueRating), 0.0) " +
            "FROM Review r WHERE r.hotel.id = :hotelId")
    List<Object[]> getRatingStatisticsByHotelId(@Param("hotelId") Long hotelId);
}
