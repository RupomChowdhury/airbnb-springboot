package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.HotelRequest;
import dev.rupom.project.airbnb.dto.HotelResponse;
import dev.rupom.project.airbnb.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
