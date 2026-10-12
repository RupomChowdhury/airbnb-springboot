package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.dto.PayoutResponse;
import dev.rupom.project.airbnb.entity.Booking;
import dev.rupom.project.airbnb.entity.Payout;
import dev.rupom.project.airbnb.entity.User;
import dev.rupom.project.airbnb.entity.enums.PayoutStatus;
import dev.rupom.project.airbnb.exception.BadRequestException;
import dev.rupom.project.airbnb.exception.ResourceNotFoundException;
import dev.rupom.project.airbnb.mapper.PayoutMapper;
import dev.rupom.project.airbnb.repository.PayoutRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import dev.rupom.project.airbnb.dto.event.PayoutEvent;
import dev.rupom.project.airbnb.dto.event.PayoutEventType;
import dev.rupom.project.airbnb.service.kafka.KafkaEventProducer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRepository payoutRepository;
    private final PayoutMapper payoutMapper;
    private final KafkaEventProducer kafkaEventProducer;

    @Transactional
    @Override
    public Payout createEscrowPayout(Booking booking) {
        log.info("Creating escrow payout for booking id: {}", booking.getId());

        User host = booking.getHotel().getOwner();
        if (host == null) {
            log.warn("Hotel id: {} has no assigned owner. Escrow payout cannot be linked to host.", booking.getHotel().getId());
            return null;
        }

        LocalDate releaseDate = booking.getCheckInDate().plusDays(1);
        BigDecimal amount = booking.getHostPayout() != null ? booking.getHostPayout() : BigDecimal.ZERO;

        Payout payout = Payout.builder()
                .booking(booking)
                .host(host)
                .amount(amount)
                .status(PayoutStatus.PENDING_ESCROW)
                .releaseDate(releaseDate)
                .build();

        Payout saved = payoutRepository.save(payout);
        log.info("Escrow payout created with id: {}, amount: {}, scheduled release: {}", saved.getId(), amount, releaseDate);

        // Publish Kafka PayoutEvent (CREATED)
        kafkaEventProducer.publishPayoutEvent(PayoutEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(PayoutEventType.CREATED)
                .payoutId(saved.getId())
                .bookingId(booking.getId())
                .hostId(host.getId())
                .hostName(host.getName())
                .hostEmail(host.getEmail())
                .amount(saved.getAmount())
                .releaseDate(saved.getReleaseDate())
                .timestamp(Instant.now())
                .build());

        return saved;
    }

    @Transactional
    @Override
    public void cancelEscrowPayout(Long bookingId) {
        log.info("Cancelling escrow payout for booking id: {}", bookingId);
        payoutRepository.findByBookingId(bookingId).ifPresent(payout -> {
            payout.setStatus(PayoutStatus.CANCELLED);
            payoutRepository.save(payout);
            log.info("Escrow payout id: {} for booking id: {} cancelled.", payout.getId(), bookingId);

            // Publish Kafka PayoutEvent (CANCELLED)
            if (payout.getHost() != null) {
                kafkaEventProducer.publishPayoutEvent(PayoutEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType(PayoutEventType.CANCELLED)
                        .payoutId(payout.getId())
                        .bookingId(bookingId)
                        .hostId(payout.getHost().getId())
                        .hostName(payout.getHost().getName())
                        .hostEmail(payout.getHost().getEmail())
                        .amount(payout.getAmount())
                        .releaseDate(payout.getReleaseDate())
                        .timestamp(Instant.now())
                        .build());
            }
        });
    }

    @Transactional
    @Override
    public void updatePayoutAmount(Long bookingId, BigDecimal newAmount) {
        log.info("Updating payout amount for booking id: {} to {}", bookingId, newAmount);
        payoutRepository.findByBookingId(bookingId).ifPresent(payout -> {
            payout.setAmount(newAmount);
            if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
                payout.setStatus(PayoutStatus.CANCELLED);
            }
            payoutRepository.save(payout);
        });
    }

    @Scheduled(cron = "0 0 1 * * *") // Daily at 1:00 AM
    @Transactional
    @Override
    public int releaseDuePayouts() {
        LocalDate today = LocalDate.now();
        log.info("Running daily escrow payout release sweep for date <= {}", today);

        List<Payout> duePayouts = payoutRepository.findByStatusAndReleaseDateLessThanEqual(PayoutStatus.PENDING_ESCROW, today);
        if (duePayouts.isEmpty()) {
            log.info("No escrow payouts due for release today.");
            return 0;
        }

        log.info("Found {} escrow payouts ready for release", duePayouts.size());
        LocalDateTime now = LocalDateTime.now();

        for (Payout payout : duePayouts) {
            payout.setStatus(PayoutStatus.RELEASED);
            payout.setReleasedAt(now);
            log.info("Released escrow payout id: {} to host id: {} for amount: {}",
                    payout.getId(), payout.getHost().getId(), payout.getAmount());

            // Publish Kafka PayoutEvent (RELEASED)
            kafkaEventProducer.publishPayoutEvent(PayoutEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(PayoutEventType.RELEASED)
                    .payoutId(payout.getId())
                    .bookingId(payout.getBooking() != null ? payout.getBooking().getId() : null)
                    .hostId(payout.getHost().getId())
                    .hostName(payout.getHost().getName())
                    .hostEmail(payout.getHost().getEmail())
                    .amount(payout.getAmount())
                    .releaseDate(payout.getReleaseDate())
                    .timestamp(Instant.now())
                    .build());
        }

        payoutRepository.saveAll(duePayouts);
        log.info("Successfully processed and released {} host escrow payouts", duePayouts.size());
        return duePayouts.size();
    }

    @Transactional
    @Override
    public PayoutResponse releasePayoutManually(Long payoutId) {
        log.info("Admin manual release of payout id: {}", payoutId);
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found with id: " + payoutId));

        if (payout.getStatus() == PayoutStatus.RELEASED) {
            throw new BadRequestException("Payout is already released.");
        }
        if (payout.getStatus() == PayoutStatus.CANCELLED) {
            throw new BadRequestException("Cannot release a cancelled payout.");
        }

        payout.setStatus(PayoutStatus.RELEASED);
        payout.setReleasedAt(LocalDateTime.now());
        Payout saved = payoutRepository.save(payout);

        // Publish Kafka PayoutEvent (RELEASED)
        kafkaEventProducer.publishPayoutEvent(PayoutEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(PayoutEventType.RELEASED)
                .payoutId(saved.getId())
                .bookingId(saved.getBooking() != null ? saved.getBooking().getId() : null)
                .hostId(saved.getHost().getId())
                .hostName(saved.getHost().getName())
                .hostEmail(saved.getHost().getEmail())
                .amount(saved.getAmount())
                .releaseDate(saved.getReleaseDate())
                .timestamp(Instant.now())
                .build());

        return payoutMapper.toPayoutResponse(saved);
    }

    @Override
    public List<PayoutResponse> getPayoutsByHost(Long hostId) {
        log.info("Fetching payouts for host id: {}", hostId);
        return payoutRepository.findByHostIdOrderByCreatedAtDesc(hostId).stream()
                .map(payoutMapper::toPayoutResponse)
                .toList();
    }

    @Override
    public List<PayoutResponse> getAllPayouts() {
        log.info("Fetching all system payouts");
        return payoutRepository.findAll().stream()
                .map(payoutMapper::toPayoutResponse)
                .toList();
    }
}
