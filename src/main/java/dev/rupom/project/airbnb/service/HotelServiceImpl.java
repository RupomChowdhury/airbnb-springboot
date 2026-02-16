package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.HotelRequest;
import dev.rupom.project.airbnb.dto.HotelResponse;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.HotelMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements  HotelService {
    private final HotelRepository hotelRepository;
    private final HotelMapper mapper;
    private final ObjectMapper objectMapper;
    private static final String HOTEL_NOT_FOUND = "Hotel not found.";

    @Transactional
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
                .orElseThrow(()-> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        log.info("Hotel with id: {} has been found",hotel.getId());
        return  mapper.toHotelResponse(hotel);
    }

    @Override
    public List<HotelResponse> getAllHotels() {
        log.info("Getting all hotels");
        return hotelRepository.findAll()
                .stream()
                .map(mapper::toHotelResponse)
                .toList();
    }

    @Transactional
    @Override
    public HotelResponse updateHotelById(Long id, HotelRequest request) {
        log.info("Updating hotel with id: {}",id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(()-> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        hotel.setName(request.getName());
        hotel.setCity(request.getCity());
        hotel.setPhotos(request.getPhotos());
        hotel.setAmenities(request.getAmenities());
        hotel.setContactInfo(request.getContactInfo());
        hotel.setActive(request.getActive());
        log.info("Updating hotel with id: {}",id);
        Hotel updatedHotel = hotelRepository.save(hotel);
        log.info("Updated hotel with id: {}",id);
        return mapper.toHotelResponse(updatedHotel);
    }

    @Transactional
    @Override
    public Boolean deleteHotelById(Long id) {
        log.info("Deleting hotel with id: {}",id);
        isExist(id);
        hotelRepository.deleteById(id);
        //TODO: delete the future inventories for this hotel.
        log.info("Deleted hotel with id: {}",id);
        return true;
    }

    @Transactional
    @Override
    public Boolean activateHotelById(Long id) {
        log.info("Activating hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        if(Boolean.TRUE.equals(hotel.getActive())){
            return true;
        }
        hotel.setActive(true);
        hotelRepository.save(hotel);
        log.info("Activated hotel with id: {}",id);
        //TODO: Create inventory for all the rooms of this hotel.
        return true;
    }

    @Transactional
    @Override
    public Boolean deactivateHotelById(Long id) {
        log.info("Deactivating hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        hotel.setActive(false);
        hotelRepository.save(hotel);
        log.info("Deactivated hotel with id: {}",id);
        return true;
    }

    @Transactional
    @Override
    public HotelResponse editHotelDetailsById(Long id, Map<String, Object> updates) {
        log.info("Editing hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        objectMapper.updateValue(hotel, updates);
        log.info("Edited hotel with id: {}",id);
        return mapper.toHotelResponse(hotelRepository.save(hotel));
    }

    void isExist(Long id){
        log.info("Checking if hotel with id: {} exists",id);
        if(!hotelRepository.existsById(id)) throw new ResourceNotFoundException(HOTEL_NOT_FOUND);
    }
}
