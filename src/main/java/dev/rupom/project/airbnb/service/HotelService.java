package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.HotelRequest;
import dev.rupom.project.airbnb.dto.HotelResponse;

import java.util.List;
import java.util.Map;

public interface HotelService {
    HotelResponse createNewHotel(HotelRequest hotelDTO);
    HotelResponse getHotelById(Long id);
    List<HotelResponse> getAllHotels();
    HotelResponse updateHotelById(Long id, HotelRequest request);
    Boolean deleteHotelById(Long id);
    Boolean activateHotelById(Long id);
    Boolean deactivateHotelById(Long id);
    HotelResponse editHotelDetailsById(Long id, Map<String,Object> updates);
}
