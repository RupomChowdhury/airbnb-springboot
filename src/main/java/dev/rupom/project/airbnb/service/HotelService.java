package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.HotelRequest;
import dev.rupom.project.airbnb.dto.HotelResponse;

public interface HotelService {
    HotelResponse createNewHotel(HotelRequest hotelDTO);
    HotelResponse getHotelById(Long id);
}
