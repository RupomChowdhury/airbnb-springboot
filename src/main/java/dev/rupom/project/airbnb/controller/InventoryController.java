package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.InventoryGenerationRequest;
import dev.rupom.project.airbnb.dto.InventoryResponse;
import dev.rupom.project.airbnb.dto.InventoryStatusRequest;
import dev.rupom.project.airbnb.dto.InventorySurgeRequest;
import dev.rupom.project.airbnb.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms/{roomId}/inventories")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getInventoriesByRoomAndDateRange(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Attempting to get inventories for hotel: {}, room: {} between {} and {}", hotelId, roomId, startDate, endDate);
        return ResponseEntity.ok(inventoryService.getInventoriesByRoomAndDateRange(hotelId, roomId, startDate, endDate));
    }

    @PatchMapping("/surge")
    public ResponseEntity<Boolean> updateSurgeFactor(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @RequestBody @Valid InventorySurgeRequest request) {
        log.info("Attempting to update surge factor for hotel: {}, room: {}", hotelId, roomId);
        return ResponseEntity.ok(inventoryService.updateSurgeFactor(hotelId, roomId, request));
    }

    @PatchMapping("/status")
    public ResponseEntity<Boolean> updateInventoryStatus(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @RequestBody @Valid InventoryStatusRequest request) {
        log.info("Attempting to update inventory status for hotel: {}, room: {}", hotelId, roomId);
        return ResponseEntity.ok(inventoryService.updateInventoryStatus(hotelId, roomId, request));
    }

    @PostMapping("/generate")
    public ResponseEntity<Boolean> generateCustomInventory(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @RequestBody @Valid InventoryGenerationRequest request) {
        log.info("Attempting to generate custom inventory for hotel: {}, room: {}", hotelId, roomId);
        return ResponseEntity.ok(inventoryService.generateCustomInventory(hotelId, roomId, request));
    }
}
