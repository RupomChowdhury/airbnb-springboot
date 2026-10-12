package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.config.RedisCacheConfig;
import dev.rupom.project.airbnb.dto.HotelRequest;
import dev.rupom.project.airbnb.dto.HotelResponse;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.HotelMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.UserRepository;
import dev.rupom.project.airbnb.util.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {
    private final HotelRepository hotelRepository;
    private final HotelMapper mapper;
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final UserRepository userRepository;
    private static final String HOTEL_NOT_FOUND = "Hotel not found.";

    @Transactional
    @Override
    public HotelResponse createNewHotel(HotelRequest request) {
        log.info("Creating a new hotel with name: {}", request.getName());
        Hotel hotel = mapper.toHotel(request);
        if (hotel.getActive() == null) {
            hotel.setActive(false);
        }

        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId != null) {
                User owner = userRepository.findById(currentUserId).orElse(null);
                hotel.setOwner(owner);
            }
        } catch (Exception ex) {
            log.debug("No authenticated user context found when creating hotel: {}", ex.getMessage());
        }

        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel with id: {} has been created", savedHotel.getId());
        return mapper.toHotelResponse(savedHotel);
    }

    @Cacheable(value = RedisCacheConfig.HOTEL_DETAILS_CACHE, key = "#id")
    @Override
    public HotelResponse getHotelById(Long id) {
        log.info("Getting hotel with id: {}", id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        log.info("Hotel with id: {} has been found", hotel.getId());
        return mapper.toHotelResponse(hotel);
    }

    @Override
    public List<HotelResponse> getAllHotels() {
        log.info("Getting all hotels");
        return hotelRepository.findAll()
                .stream()
                .map(mapper::toHotelResponse)
                .toList();
    }

    @CacheEvict(value = RedisCacheConfig.HOTEL_DETAILS_CACHE, key = "#id")
    @Transactional
    @Override
    public HotelResponse updateHotelById(Long id, HotelRequest request) {
        log.info("Updating hotel with id: {}", id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        hotel.setName(request.getName());
        hotel.setCity(request.getCity());
        hotel.setPhotos(request.getPhotos());
        hotel.setAmenities(request.getAmenities());
        hotel.setContactInfo(request.getContactInfo());
        hotel.setActive(request.getActive());
        if (request.getLatitude() != null) hotel.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) hotel.setLongitude(request.getLongitude());
        if (request.getCleaningFee() != null) hotel.setCleaningFee(request.getCleaningFee());
        if (request.getWeeklyDiscountPercent() != null) hotel.setWeeklyDiscountPercent(request.getWeeklyDiscountPercent());
        if (request.getCancellationPolicy() != null) hotel.setCancellationPolicy(request.getCancellationPolicy());
        if (request.getInstantBookable() != null) hotel.setInstantBookable(request.getInstantBookable());
        log.info("Updating hotel with id: {}", id);
        Hotel updatedHotel = hotelRepository.save(hotel);
        log.info("Updated hotel with id: {}", id);
        return mapper.toHotelResponse(updatedHotel);
    }

    @CacheEvict(value = RedisCacheConfig.HOTEL_DETAILS_CACHE, key = "#id")
    @Transactional
    @Override
    public Boolean deleteHotelById(Long id) {
        log.info("Deleting hotel with id: {}", id);
        isExist(id);
        // Delete the future inventories for this hotel
        inventoryService.deleteFutureInventoriesForHotel(id);
        hotelRepository.deleteById(id);
        log.info("Deleted hotel with id: {}", id);
        return true;
    }

    @CacheEvict(value = RedisCacheConfig.HOTEL_DETAILS_CACHE, key = "#id")
    @Transactional
    @Override
    public Boolean activateHotelById(Long id) {
        log.info("Activating hotel with id: {}", id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        if (Boolean.TRUE.equals(hotel.getActive())) {
            return true;
        }
        hotel.setActive(true);
        hotelRepository.save(hotel);
        log.info("Activated hotel with id: {}", id);
        // Create inventory for all the rooms of this hotel
        inventoryService.initializeHotelInventory(hotel, LocalDate.now(), LocalDate.now().plusDays(365));
        return true;
    }

    @CacheEvict(value = RedisCacheConfig.HOTEL_DETAILS_CACHE, key = "#id")
    @Transactional
    @Override
    public Boolean deactivateHotelById(Long id) {
        log.info("Deactivating hotel with id: {}", id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        hotel.setActive(false);
        hotelRepository.save(hotel);
        log.info("Deactivated hotel with id: {}", id);
        return true;
    }

    @CacheEvict(value = RedisCacheConfig.HOTEL_DETAILS_CACHE, key = "#id")
    @Transactional
    @Override
    public HotelResponse editHotelDetailsById(Long id, Map<String, Object> updates) {
        log.info("Editing hotel with id: {}", id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        objectMapper.updateValue(hotel, updates);
        log.info("Edited hotel with id: {}", id);
        return mapper.toHotelResponse(hotelRepository.save(hotel));
    }

    void isExist(Long id) {
        log.info("Checking if hotel with id: {} exists", id);
        if (!hotelRepository.existsById(id)) throw new ResourceNotFoundException(HOTEL_NOT_FOUND);
    }
}
