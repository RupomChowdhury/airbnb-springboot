package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.HotelRequest;
import dev.rupom.project.airbnb.dto.HotelResponse;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.HotelMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements  HotelService {
    private final HotelRepository hotelRepository;
    private final HotelMapper mapper;


    @Override
    public HotelResponse createNewHotel(HotelRequest request) {
        log.info("Creating a new hotel with name: {}",request.getName());
        Hotel savedHotel = hotelRepository.save(mapper.toHotel(request));
        log.info("Hotel with id: {} has been created",savedHotel.getId());
        return mapper.toHotelResponse(savedHotel);
    }

    @Override
    public HotelResponse getHotelById(Long id) {
        log.info("Getting hotel with id: {}",id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Hotel not found"));
        log.info("Hotel with id: {} has been found",hotel.getId());
        return  mapper.toHotelResponse(hotel);
    }
}
