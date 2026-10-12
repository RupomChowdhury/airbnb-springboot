package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.dto.HotelPriceQuoteResponse;
import dev.rupom.project.airbnb.dto.HotelResponse;
import dev.rupom.project.airbnb.dto.HotelSearchRequest;
import dev.rupom.project.airbnb.dto.HotelSearchResponse;
import dev.rupom.project.airbnb.service.HotelSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelBrowseController {

    private final HotelSearchService hotelSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<HotelSearchResponse>> searchHotels(@ModelAttribute @Valid HotelSearchRequest request) {
        log.info("Searching hotels for city: {}, checkIn: {}, checkOut: {}",
                request.getCity(), request.getCheckInDate(), request.getCheckOutDate());
        return ResponseEntity.ok(hotelSearchService.searchHotels(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelResponse> getHotelDetails(@PathVariable Long id) {
        log.info("Getting hotel details for id: {}", id);
        return ResponseEntity.ok(hotelSearchService.getHotelDetails(id));
    }

    @GetMapping("/{hotelId}/rooms/{roomId}/pricing")
    public ResponseEntity<HotelPriceQuoteResponse> getPriceQuote(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam(required = false, defaultValue = "1") Integer roomsCount) {
        log.info("Getting price quote for hotel: {}, room: {} between {} and {}", hotelId, roomId, checkInDate, checkOutDate);
        return ResponseEntity.ok(hotelSearchService.getRoomPriceQuote(hotelId, roomId, checkInDate, checkOutDate, roomsCount));
    }
}
