package dev.rupom.project.airbnb.config;

import dev.rupom.project.airbnb.entity.*;
import dev.rupom.project.airbnb.entity.enums.*;
import dev.rupom.project.airbnb.repository.*;
import dev.rupom.project.airbnb.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data-initializer.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PayoutRepository payoutRepository;
    private final ReviewRepository reviewRepository;
    private final InventoryService inventoryService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist in database (count: {}). Skipping sample data initialization.", userRepository.count());
            return;
        }

        log.info("Starting turnkey sample data seeding for Airbnb clone...");

        // 1. Seed Core Users
        User admin = User.builder()
                .name("System Administrator")
                .email("admin@airbnb.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(UserRole.Admin, UserRole.Manager, UserRole.User))
                .build();

        User host = User.builder()
                .name("Elena Rostova")
                .email("host@airbnb.com")
                .password(passwordEncoder.encode("host123"))
                .roles(Set.of(UserRole.Manager, UserRole.User))
                .build();

        User guest = User.builder()
                .name("John Doe")
                .email("guest@airbnb.com")
                .password(passwordEncoder.encode("guest123"))
                .roles(Set.of(UserRole.User))
                .build();

        userRepository.saveAll(List.of(admin, host, guest));
        log.info("Seeded 3 users: admin@airbnb.com (Admin), host@airbnb.com (Host), guest@airbnb.com (Guest)");

        // 2. Hotel 1: Paris Penthouse
        Hotel hotel1 = Hotel.builder()
                .name("The Eiffel Luxury Penthouse")
                .city("Paris")
                .latitude(48.8584)
                .longitude(2.2945)
                .cleaningFee(new BigDecimal("75.00"))
                .weeklyDiscountPercent(new BigDecimal("10.00"))
                .cancellationPolicy(CancellationPolicy.FLEXIBLE)
                .instantBookable(true)
                .active(true)
                .amenities(new String[]{"WiFi", "Balcony", "Air Conditioning", "Kitchen", "Elevator", "Eiffel Tower View"})
                .photos(new String[]{
                        "https://images.unsplash.com/photo-1502602898657-3e91760cbb34",
                        "https://images.unsplash.com/photo-1549144511-f099e773c147"
                })
                .contactInfo(ContactInfo.builder()
                        .completeAddress("Avenue Gustave Eiffel, 75007 Paris, France")
                        .location("Paris 7th Arrondissement")
                        .email("host@airbnb.com")
                        .phoneNumber("+33 1 42 68 55 00")
                        .build())
                .owner(host)
                .build();
        hotel1 = hotelRepository.save(hotel1);

        Room room1 = Room.builder()
                .hotel(hotel1)
                .type("Penthouse Suite")
                .basePrice(new BigDecimal("250.00"))
                .capacity(2)
                .totalCount(3)
                .amenities(new String[]{"King Bed", "Ensuite Jacuzzi", "Balcony View", "Espresso Machine"})
                .photos(new String[]{"https://images.unsplash.com/photo-1582719478250-c89cae4dc85b"})
                .build();

        Room room2 = Room.builder()
                .hotel(hotel1)
                .type("Deluxe Queen Room")
                .basePrice(new BigDecimal("150.00"))
                .capacity(2)
                .totalCount(5)
                .amenities(new String[]{"Queen Bed", "City View", "Work Desk", "Fast WiFi"})
                .photos(new String[]{"https://images.unsplash.com/photo-1590490360182-c33d57733427"})
                .build();

        room1 = roomRepository.save(room1);
        room2 = roomRepository.save(room2);
        hotel1.setRooms(List.of(room1, room2));

        // 3. Hotel 2: Santorini Cliffside Villa
        Hotel hotel2 = Hotel.builder()
                .name("Santorini Cliffside Caldera Villa")
                .city("Santorini")
                .latitude(36.4618)
                .longitude(25.3753)
                .cleaningFee(new BigDecimal("100.00"))
                .weeklyDiscountPercent(new BigDecimal("15.00"))
                .cancellationPolicy(CancellationPolicy.MODERATE)
                .instantBookable(true)
                .active(true)
                .amenities(new String[]{"Infinity Pool", "Sea View", "Sunset Terrace", "Breakfast Included", "WiFi"})
                .photos(new String[]{"https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff"})
                .contactInfo(ContactInfo.builder()
                        .completeAddress("Oia Caldera Walk, 84702 Santorini, Greece")
                        .location("Oia Cliffside")
                        .email("santorini@airbnb.com")
                        .phoneNumber("+30 2286 071234")
                        .build())
                .owner(host)
                .build();
        hotel2 = hotelRepository.save(hotel2);

        Room room3 = Room.builder()
                .hotel(hotel2)
                .type("Cave Villa Master")
                .basePrice(new BigDecimal("350.00"))
                .capacity(4)
                .totalCount(2)
                .amenities(new String[]{"Private Plunge Pool", "King Bed", "Caldera Sunset View"})
                .photos(new String[]{"https://images.unsplash.com/photo-1512917774080-9991f1c4c750"})
                .build();
        room3 = roomRepository.save(room3);
        hotel2.setRooms(List.of(room3));

        // 4. Hotel 3: Zermatt Alpine Chalet
        Hotel hotel3 = Hotel.builder()
                .name("Matterhorn Alpine Chalet")
                .city("Zermatt")
                .latitude(45.9763)
                .longitude(7.7491)
                .cleaningFee(new BigDecimal("120.00"))
                .weeklyDiscountPercent(new BigDecimal("12.00"))
                .cancellationPolicy(CancellationPolicy.STRICT)
                .instantBookable(true)
                .active(true)
                .amenities(new String[]{"Ski-in / Ski-out", "Fireplace", "Hot Tub", "Sauna", "Mountain View"})
                .photos(new String[]{"https://images.unsplash.com/photo-1502784444187-359ac186c5bb"})
                .contactInfo(ContactInfo.builder()
                        .completeAddress("Bahnhofstrasse 12, 3920 Zermatt, Switzerland")
                        .location("Zermatt Village")
                        .email("zermatt@airbnb.com")
                        .phoneNumber("+41 27 966 8100")
                        .build())
                .owner(host)
                .build();
        hotel3 = hotelRepository.save(hotel3);

        Room room4 = Room.builder()
                .hotel(hotel3)
                .type("Alpine Chalet Loft")
                .basePrice(new BigDecimal("400.00"))
                .capacity(6)
                .totalCount(2)
                .amenities(new String[]{"Wood Fireplace", "Cedar Sauna", "Panoramic Balcony"})
                .photos(new String[]{"https://images.unsplash.com/photo-1542314831-068cd1dbfeeb"})
                .build();
        room4 = roomRepository.save(room4);
        hotel3.setRooms(List.of(room4));

        log.info("Seeded 3 scenic hotels (Paris, Santorini, Zermatt) with luxury rooms");

        // 5. Initialize 90-Day Inventories
        LocalDate today = LocalDate.now();
        LocalDate future90Days = today.plusDays(90);
        inventoryService.initializeHotelInventory(hotel1, today, future90Days);
        inventoryService.initializeHotelInventory(hotel2, today, future90Days);
        inventoryService.initializeHotelInventory(hotel3, today, future90Days);
        log.info("Initialized 90-day room inventories for all hotels");

        // 6. Seed Past Confirmed Stay, Payment, Released Payout, and 5-Star Review
        LocalDate checkIn = today.minusDays(7);
        LocalDate checkOut = today.minusDays(3);

        Booking pastBooking = Booking.builder()
                .hotel(hotel1)
                .room(room1)
                .user(guest)
                .status(BookingStatus.CONFIRMED)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .roomsCount(1)
                .nightlyTotal(new BigDecimal("1000.00"))
                .cleaningFee(new BigDecimal("75.00"))
                .serviceFee(new BigDecimal("140.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(new BigDecimal("65.00"))
                .totalPrice(new BigDecimal("1280.00"))
                .hostPayout(new BigDecimal("1241.60"))
                .build();
        pastBooking = bookingRepository.save(pastBooking);

        Payment payment = Payment.builder()
                .booking(pastBooking)
                .price(new BigDecimal("1280.00"))
                .status(PaymentStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionId("TXN-SEED-PARIS-001")
                .build();
        payment = paymentRepository.save(payment);
        pastBooking.setPayment(payment);
        bookingRepository.save(pastBooking);

        Payout payout = Payout.builder()
                .booking(pastBooking)
                .host(host)
                .amount(new BigDecimal("1241.60"))
                .status(PayoutStatus.RELEASED)
                .releaseDate(today.minusDays(6))
                .releasedAt(LocalDateTime.now().minusDays(6))
                .build();
        payoutRepository.save(payout);

        Review review = Review.builder()
                .user(guest)
                .hotel(hotel1)
                .booking(pastBooking)
                .overallRating(5)
                .cleanlinessRating(5)
                .accuracyRating(5)
                .communicationRating(5)
                .locationRating(5)
                .checkInRating(5)
                .valueRating(5)
                .comment("Unbelievable view of the Eiffel Tower from the balcony! Everything was sparkling clean and the host was wonderfully helpful.")
                .build();
        reviewRepository.save(review);

        log.info("Turnkey sample data seeding successfully finished! 3 Users, 3 Hotels, Inventories, Payment, Payout, and 5-Star Review ready.");
    }
}
