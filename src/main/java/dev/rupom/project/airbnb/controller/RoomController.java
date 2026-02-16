package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.RoomRequest;
import dev.rupom.project.airbnb.dto.RoomResponse;
import dev.rupom.project.airbnb.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createNewRoom(@PathVariable Long hotelId, @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createNewRoom(hotelId, request));
    }
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRoomsByHotelId(@PathVariable Long hotelId){
        return ResponseEntity.ok(roomService.getAllRoomsByHotelId(hotelId));
    }
    @GetMapping(path = "/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long hotelId, @PathVariable Long roomId){
        return ResponseEntity.ok(roomService.getRoomById(hotelId, roomId));
    }
    @DeleteMapping(path = "/{roomId}")
    public ResponseEntity<Boolean> deleteRoomById(@PathVariable Long hotelId, @PathVariable Long roomId){
        return ResponseEntity.ok(roomService.deleteRoomById(hotelId, roomId));
    }
}
