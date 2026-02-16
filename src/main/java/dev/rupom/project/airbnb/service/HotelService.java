package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.HotelRequest;
import dev.rupom.project.airbnb.dto.HotelResponse;

import java.util.List;

public interface HotelService {
    HotelResponse createNewHotel(HotelRequest hotelDTO);
    HotelResponse getHotelById(Long id);
    List<HotelResponse> getAllHotels();
    HotelResponse updateHotelById(Long id, HotelRequest request);
    Boolean deleteHotelById(Long id);
}
