package dev.rupom.project.airbnb.config;

import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Room;
import dev.rupom.project.airbnb.repository.*;
import dev.rupom.project.airbnb.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_whenUsersAlreadyExist_skipsSeeding() {
        when(userRepository.count()).thenReturn(3L);

        dataInitializer.run();

        verify(userRepository, never()).saveAll(any());
        verify(hotelRepository, never()).save(any());
        verify(inventoryService, never()).initializeHotelInventory(any(), any(), any());
    }

    @Test
    void run_whenDatabaseEmpty_seedsTurnkeyData() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(hotelRepository.save(any(Hotel.class))).thenAnswer(i -> {
            Hotel h = i.getArgument(0);
            h.setId(1L);
            return h;
        });
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> {
            Room r = i.getArgument(0);
            r.setId(10L);
            return r;
        });
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        dataInitializer.run();

        verify(userRepository, times(1)).saveAll(any(List.class));
        verify(hotelRepository, times(3)).save(any(Hotel.class));
        verify(inventoryService, times(3)).initializeHotelInventory(any(), any(), any());
        verify(bookingRepository, atLeastOnce()).save(any());
        verify(paymentRepository, times(1)).save(any());
        verify(payoutRepository, times(1)).save(any());
        verify(reviewRepository, times(1)).save(any());
    }
}
