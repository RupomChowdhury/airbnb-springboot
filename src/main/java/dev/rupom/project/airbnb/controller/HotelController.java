package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.HotelRequest;
import dev.rupom.project.airbnb.dto.HotelResponse;
import dev.rupom.project.airbnb.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelController {
    private final HotelService hotelService;

    @PostMapping
    public ResponseEntity<HotelResponse> createNewHotel(@RequestBody HotelRequest request){
        log.info("Attempting to create a new hotel with name: {}",request.getName());
        HotelResponse hotelResponse = hotelService.createNewHotel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelResponse);
    }
    @GetMapping("/{id}")
    public ResponseEntity<HotelResponse> getHotelById(@PathVariable Long id){
        log.info("Attempting to get hotel with id: {}",id);
        return ResponseEntity.ok(hotelService.getHotelById(id));
    }
    @GetMapping
    public ResponseEntity<List<HotelResponse>> getAllHotels(){
        return ResponseEntity.ok(hotelService.getAllHotels());
    }
    @PutMapping(path = "/{id}")
    public ResponseEntity<HotelResponse> updateHotelById(@PathVariable Long id, @RequestBody HotelRequest request){
        log.info("Attempting to update hotel with id: {}",id);
        return ResponseEntity.ok(hotelService.updateHotelById(id, request));
    }
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Boolean> deleteHotelById(@PathVariable Long id){
        log.info("Attempting to delete hotel with id: {}",id);
        return ResponseEntity.ok(hotelService.deleteHotelById(id));
    }
    @PatchMapping(path = "activate/{id}")
    public ResponseEntity<Boolean> activeHotelById(@PathVariable Long id){
        return ResponseEntity.ok(hotelService.activeHotelById(id));
    }
    @PatchMapping(path = "/deactivate/{id}")
    public ResponseEntity<Boolean> deactivateHotelById(@PathVariable Long id){
        return ResponseEntity.ok(hotelService.deactiveHotelById(id));
    }
    @PatchMapping(path = "/{id}")
    public ResponseEntity<HotelResponse> editHotelById(@PathVariable Long id, @RequestBody Map<String,Object> request){
        return ResponseEntity.ok(hotelService.editHotelDetailsById(id, request));
    }
}
