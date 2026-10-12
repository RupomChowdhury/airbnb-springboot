package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
    List<Guest> findByUserId(Long userId);
}
