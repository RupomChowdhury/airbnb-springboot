package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.HotelPriceQuoteResponse;
import dev.rupom.project.airbnb.dto.HotelResponse;
import dev.rupom.project.airbnb.dto.HotelSearchRequest;
import dev.rupom.project.airbnb.dto.HotelSearchResponse;

import java.time.LocalDate;
import java.util.List;

public interface HotelSearchService {
    List<HotelSearchResponse> searchHotels(HotelSearchRequest request);
    HotelResponse getHotelDetails(Long hotelId);
    HotelPriceQuoteResponse getRoomPriceQuote(Long hotelId, Long roomId, LocalDate checkInDate, LocalDate checkOutDate, Integer roomsCount);
}
