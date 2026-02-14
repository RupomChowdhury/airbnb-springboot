package dev.rupom.project.airbnb.repository;

import dev.rupom.project.airbnb.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
