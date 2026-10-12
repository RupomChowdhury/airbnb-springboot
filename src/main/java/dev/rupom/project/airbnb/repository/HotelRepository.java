package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    List<Hotel> findByCityIgnoreCaseAndActiveTrue(String city);
    List<Hotel> findByActiveTrue();
    List<Hotel> findByOwnerId(Long ownerId);
    Optional<Hotel> findByNameIgnoreCase(String name);
    List<Hotel> findByActiveTrueAndLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng
    );
}
