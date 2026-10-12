package dev.rupom.project.airbnb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.rupom.project.airbnb.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class PaymentIdempotencyService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:payment:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    public PaymentIdempotencyService(
            @Autowired(required = false) StringRedisTemplate stringRedisTemplate,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = (objectMapper != null)
                ? objectMapper
                : new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * Checks if a payment response is already cached for the given user and idempotency key.
     */
    public Optional<PaymentResponse> getCachedPayment(Long userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || stringRedisTemplate == null) {
            return Optional.empty();
        }

        String key = buildKey(userId, idempotencyKey);
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(key);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.info("Found cached idempotent payment response for key: {}", key);
                PaymentResponse response = objectMapper.readValue(cachedJson, PaymentResponse.class);
                return Optional.ofNullable(response);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve cached idempotency key [{}] from Redis: {}. Proceeding with normal processing.", key, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Caches the completed payment response under the idempotency key for 24 hours.
     */
    public void cachePayment(Long userId, String idempotencyKey, PaymentResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || response == null || stringRedisTemplate == null) {
            return;
        }

        String key = buildKey(userId, idempotencyKey);
        try {
            String json = objectMapper.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(key, json, IDEMPOTENCY_TTL);
            log.info("Successfully cached idempotent payment response for key: {} (TTL: 24h)", key);
        } catch (Exception e) {
            log.warn("Failed to cache payment under idempotency key [{}] in Redis: {}", key, e.getMessage());
        }
    }

    private String buildKey(Long userId, String idempotencyKey) {
        return IDEMPOTENCY_PREFIX + userId + ":" + idempotencyKey.trim();
    }
}
