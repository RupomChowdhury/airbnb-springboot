package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.RoomRequest;
import dev.rupom.project.airbnb.dto.RoomResponse;
import dev.rupom.project.airbnb.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createNewRoom(@PathVariable Long hotelId, @RequestBody @Valid RoomRequest request) {
        log.info("Attempting to create room for hotel id: {}", hotelId);
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createNewRoom(hotelId, request));
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRoomsByHotelId(@PathVariable Long hotelId) {
        log.info("Attempting to get all rooms for hotel id: {}", hotelId);
        return ResponseEntity.ok(roomService.getAllRoomsByHotelId(hotelId));
    }

    @GetMapping(path = "/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long hotelId, @PathVariable Long roomId) {
        log.info("Attempting to get room with id: {} for hotel id: {}", roomId, hotelId);
        return ResponseEntity.ok(roomService.getRoomById(hotelId, roomId));
    }

    @PutMapping(path = "/{roomId}")
    public ResponseEntity<RoomResponse> updateRoomById(@PathVariable Long hotelId, @PathVariable Long roomId, @RequestBody @Valid RoomRequest request) {
        log.info("Attempting to update room with id: {} for hotel id: {}", roomId, hotelId);
        return ResponseEntity.ok(roomService.updateRoomById(hotelId, roomId, request));
    }

    @DeleteMapping(path = "/{roomId}")
    public ResponseEntity<Boolean> deleteRoomById(@PathVariable Long hotelId, @PathVariable Long roomId) {
        log.info("Attempting to delete room with id: {} for hotel id: {}", roomId, hotelId);
        return ResponseEntity.ok(roomService.deleteRoomById(hotelId, roomId));
    }
}
