package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.*;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Inventory;
import dev.rupom.project.airbnb.entity.Room;
import dev.rupom.project.airbnb.exception.BadRequestException;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.HotelMapper;
import dev.rupom.project.airbnb.repository.HotelRepository;
import dev.rupom.project.airbnb.repository.InventoryRepository;
import dev.rupom.project.airbnb.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelSearchServiceImpl implements HotelSearchService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final HotelMapper hotelMapper;
    private final ReviewService reviewService;
    private final PricingService pricingService;

    private static final String HOTEL_NOT_FOUND = "Hotel not found.";
    private static final String ROOM_NOT_FOUND = "Room not found.";

    @Override
    public List<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        log.info("Searching hotels - city: {}, lat: {}, lng: {}, checkIn: {}, checkOut: {}",
                request.getCity(), request.getLatitude(), request.getLongitude(), request.getCheckInDate(), request.getCheckOutDate());

        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        long numberOfNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        int requiredGuests = request.getGuestsCount() != null ? request.getGuestsCount() : 1;

        List<Hotel> candidateHotels = findCandidateHotels(request);
        List<HotelSearchResponse> searchResults = new ArrayList<>();

        for (Hotel hotel : candidateHotels) {
            Double distanceKm = null;
            if (request.getLatitude() != null && request.getLongitude() != null && hotel.getLatitude() != null && hotel.getLongitude() != null) {
                distanceKm = calculateHaversineDistance(request.getLatitude(), request.getLongitude(), hotel.getLatitude(), hotel.getLongitude());
                double maxRadius = (request.getRadiusKm() != null && request.getRadiusKm() > 0) ? request.getRadiusKm() : 30.0;
                if (distanceKm > maxRadius) {
                    continue; // Out of requested radius
                }
            }

            List<Room> rooms = roomRepository.findAllByHotelId(hotel.getId());
            List<RoomPriceResponse> availableRooms = new ArrayList<>();
            BigDecimal minPricePerNight = null;
            BigDecimal minTotalPrice = null;

            for (Room room : rooms) {
                // Check capacity constraint
                if (room.getCapacity() * request.getRoomsCount() < requiredGuests) {
                    continue;
                }

                List<Inventory> inventories = inventoryRepository.findByRoomIdAndDateRange(
                        room.getId(), request.getCheckInDate(), request.getCheckOutDate()
                );

                // Must have inventory for every single day in the date range
                if (inventories.size() != numberOfNights) {
                    continue;
                }

                boolean isAvailable = true;
                BigDecimal roomNightlyTotal = BigDecimal.ZERO;
                int minAvailableCount = Integer.MAX_VALUE;

                for (Inventory inventory : inventories) {
                    if (Boolean.TRUE.equals(inventory.getClosed())) {
                        isAvailable = false;
                        break;
                    }

                    int availableForDay = inventory.getTotalCount() - inventory.getBookedCount();
                    if (availableForDay < request.getRoomsCount()) {
                        isAvailable = false;
                        break;
                    }

                    minAvailableCount = Math.min(minAvailableCount, availableForDay);
                    BigDecimal dailyCost = inventory.getPrice().multiply(BigDecimal.valueOf(request.getRoomsCount()));
                    roomNightlyTotal = roomNightlyTotal.add(dailyCost);
                }

                if (isAvailable && numberOfNights > 0) {
                    BigDecimal avgPricePerNight = roomNightlyTotal.divide(BigDecimal.valueOf(numberOfNights), 2, RoundingMode.HALF_UP);

                    RoomPriceResponse roomPriceResponse = RoomPriceResponse.builder()
                            .roomId(room.getId())
                            .roomType(room.getType())
                            .basePrice(room.getBasePrice())
                            .avgPricePerNight(avgPricePerNight)
                            .totalPrice(roomNightlyTotal)
                            .capacity(room.getCapacity())
                            .availableCount(minAvailableCount)
                            .photos(room.getPhotos())
                            .amenities(room.getAmenities())
                            .build();

                    availableRooms.add(roomPriceResponse);

                    if (minPricePerNight == null || avgPricePerNight.compareTo(minPricePerNight) < 0) {
                        minPricePerNight = avgPricePerNight;
                    }
                    if (minTotalPrice == null || roomNightlyTotal.compareTo(minTotalPrice) < 0) {
                        minTotalPrice = roomNightlyTotal;
                    }
                }
            }

            if (!availableRooms.isEmpty()) {
                RatingSummaryResponse ratingSummary = reviewService.getHotelRatingSummary(hotel.getId());

                HotelSearchResponse hotelSearchResponse = HotelSearchResponse.builder()
                        .id(hotel.getId())
                        .name(hotel.getName())
                        .city(hotel.getCity())
                        .latitude(hotel.getLatitude())
                        .longitude(hotel.getLongitude())
                        .distanceKm(distanceKm != null ? Math.round(distanceKm * 10.0) / 10.0 : null)
                        .photos(hotel.getPhotos())
                        .amenities(hotel.getAmenities())
                        .contactInfo(hotel.getContactInfo())
                        .averageRating(ratingSummary.getAverageOverallRating())
                        .totalReviews(ratingSummary.getTotalReviews())
                        .isGuestFavorite(ratingSummary.getIsGuestFavorite())
                        .cleaningFee(hotel.getCleaningFee())
                        .cancellationPolicy(hotel.getCancellationPolicy())
                        .instantBookable(hotel.getInstantBookable())
                        .minPricePerNight(minPricePerNight)
                        .minTotalPrice(minTotalPrice)
                        .availableRooms(availableRooms)
                        .build();

                searchResults.add(hotelSearchResponse);
            }
        }

        // Sort by distance if coordinates provided, otherwise by min price
        if (request.getLatitude() != null && request.getLongitude() != null) {
            searchResults.sort(Comparator.comparing(
                    h -> h.getDistanceKm() != null ? h.getDistanceKm() : Double.MAX_VALUE
            ));
        }

        log.info("Found {} hotels matching search criteria", searchResults.size());
        return searchResults;
    }

    @Override
    public HotelResponse getHotelDetails(Long hotelId) {
        log.info("Getting public hotel details for hotel id: {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));

        if (!Boolean.TRUE.equals(hotel.getActive())) {
            throw new ResourceNotFoundException("Hotel is currently inactive.");
        }

        HotelResponse response = hotelMapper.toHotelResponse(hotel);
        RatingSummaryResponse ratings = reviewService.getHotelRatingSummary(hotelId);
        response.setAverageRating(ratings.getAverageOverallRating());
        response.setTotalReviews(ratings.getTotalReviews());
        response.setIsGuestFavorite(ratings.getIsGuestFavorite());
        return response;
    }

    @Override
    public HotelPriceQuoteResponse getRoomPriceQuote(Long hotelId, Long roomId, LocalDate checkInDate, LocalDate checkOutDate, Integer roomsCount) {
        log.info("Calculating price quote for hotel: {}, room: {} between {} and {}", hotelId, roomId, checkInDate, checkOutDate);
        validateDates(checkInDate, checkOutDate);

        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException(HOTEL_NOT_FOUND));
        Room room = roomRepository.findByHotelIdAndId(hotelId, roomId);
        if (room == null) {
            throw new ResourceNotFoundException(ROOM_NOT_FOUND);
        }

        int requestedRooms = (roomsCount != null && roomsCount > 0) ? roomsCount : 1;
        long numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        List<Inventory> inventories = inventoryRepository.findByRoomIdAndDateRange(roomId, checkInDate, checkOutDate);
        if (inventories.size() != numberOfNights) {
            throw new BadRequestException("Room is not available for all selected dates.");
        }

        BigDecimal nightlyTotal = BigDecimal.ZERO;
        List<DailyPriceDto> dailyBreakdown = new ArrayList<>();

        for (Inventory inventory : inventories) {
            if (Boolean.TRUE.equals(inventory.getClosed()) || (inventory.getTotalCount() - inventory.getBookedCount() < requestedRooms)) {
                throw new BadRequestException("Room is unavailable on " + inventory.getDate());
            }

            BigDecimal dailyPrice = inventory.getPrice().multiply(BigDecimal.valueOf(requestedRooms));
            nightlyTotal = nightlyTotal.add(dailyPrice);

            dailyBreakdown.add(DailyPriceDto.builder()
                    .date(inventory.getDate())
                    .price(inventory.getPrice())
                    .surgeFactor(inventory.getSurgeFactor())
                    .build());
        }

        PriceBreakdownDto priceBreakdown = pricingService.calculatePriceBreakdown(hotel, nightlyTotal, numberOfNights);
        BigDecimal avgPricePerNight = priceBreakdown.getTotalPrice().divide(BigDecimal.valueOf(numberOfNights), 2, RoundingMode.HALF_UP);

        return HotelPriceQuoteResponse.builder()
                .hotelId(hotel.getId())
                .hotelName(hotel.getName())
                .roomId(room.getId())
                .roomType(room.getType())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .totalNights(numberOfNights)
                .roomsCount(requestedRooms)
                .totalPrice(priceBreakdown.getTotalPrice())
                .avgPricePerNight(avgPricePerNight)
                .priceBreakdown(priceBreakdown)
                .cancellationPolicy(hotel.getCancellationPolicy())
                .dailyBreakdown(dailyBreakdown)
                .build();
    }

    private List<Hotel> findCandidateHotels(HotelSearchRequest request) {
        if (request.getMinLat() != null && request.getMaxLat() != null && request.getMinLng() != null && request.getMaxLng() != null) {
            return hotelRepository.findByActiveTrueAndLatitudeBetweenAndLongitudeBetween(
                    request.getMinLat(), request.getMaxLat(), request.getMinLng(), request.getMaxLng()
            );
        }

        if (request.getCity() != null && !request.getCity().trim().isEmpty()) {
            return hotelRepository.findByCityIgnoreCaseAndActiveTrue(request.getCity().trim());
        }

        return hotelRepository.findByActiveTrue();
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new BadRequestException("Check-in and check-out dates are required.");
        }
        if (checkInDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Check-in date cannot be in the past.");
        }
        if (!checkInDate.isBefore(checkOutDate)) {
            throw new BadRequestException("Check-in date must be before check-out date.");
        }
    }
}
