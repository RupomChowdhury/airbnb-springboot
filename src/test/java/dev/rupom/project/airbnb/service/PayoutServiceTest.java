package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.PayoutResponse;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Hotel;
import dev.rupom.project.airbnb.entity.Payout;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.entity.enums.PayoutStatus;
import dev.rupom.project.airbnb.mapper.PayoutMapper;
import dev.rupom.project.airbnb.repository.PayoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private PayoutMapper payoutMapper;

    @Mock
    private dev.rupom.project.airbnb.service.kafka.KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private PayoutServiceImpl payoutService;

    private User host;
    private Hotel hotel;
    private Booking booking;
    private Payout payout;

    @BeforeEach
    void setUp() {
        host = User.builder().id(10L).name("Host User").build();
        hotel = Hotel.builder().id(1L).name("Skyline Suites").owner(host).build();
        booking = Booking.builder()
                .id(100L)
                .hotel(hotel)
                .checkInDate(LocalDate.now().plusDays(2))
                .checkOutDate(LocalDate.now().plusDays(5))
                .hostPayout(BigDecimal.valueOf(350.00))
                .build();

        payout = Payout.builder()
                .id(1L)
                .booking(booking)
                .host(host)
                .amount(BigDecimal.valueOf(350.00))
                .status(PayoutStatus.PENDING_ESCROW)
                .releaseDate(LocalDate.now().plusDays(3))
                .build();
    }

    @Test
    void createEscrowPayout_success() {
        when(payoutRepository.save(any(Payout.class))).thenAnswer(i -> {
            Payout p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        Payout result = payoutService.createEscrowPayout(booking);

        assertNotNull(result);
        assertEquals(PayoutStatus.PENDING_ESCROW, result.getStatus());
        assertEquals(booking.getCheckInDate().plusDays(1), result.getReleaseDate());
        assertEquals(0, BigDecimal.valueOf(350.00).compareTo(result.getAmount()));
        verify(payoutRepository, times(1)).save(any(Payout.class));
    }

    @Test
    void cancelEscrowPayout_success() {
        when(payoutRepository.findByBookingId(100L)).thenReturn(Optional.of(payout));

        payoutService.cancelEscrowPayout(100L);

        assertEquals(PayoutStatus.CANCELLED, payout.getStatus());
        verify(payoutRepository, times(1)).save(payout);
    }

    @Test
    void releaseDuePayouts_success() {
        payout.setReleaseDate(LocalDate.now());
        when(payoutRepository.findByStatusAndReleaseDateLessThanEqual(eq(PayoutStatus.PENDING_ESCROW), any(LocalDate.class)))
                .thenReturn(List.of(payout));

        int count = payoutService.releaseDuePayouts();

        assertEquals(1, count);
        assertEquals(PayoutStatus.RELEASED, payout.getStatus());
        assertNotNull(payout.getReleasedAt());
        verify(payoutRepository, times(1)).saveAll(any());
    }

    @Test
    void releasePayoutManually_success() {
        when(payoutRepository.findById(1L)).thenReturn(Optional.of(payout));
        when(payoutRepository.save(any(Payout.class))).thenReturn(payout);
        when(payoutMapper.toPayoutResponse(payout)).thenReturn(PayoutResponse.builder()
                .id(1L)
                .status(PayoutStatus.RELEASED)
                .build());

        PayoutResponse response = payoutService.releasePayoutManually(1L);

        assertNotNull(response);
        assertEquals(PayoutStatus.RELEASED, response.getStatus());
        verify(payoutRepository, times(1)).save(payout);
    }
}
