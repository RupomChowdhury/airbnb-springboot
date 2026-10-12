package dev.rupom.project.airbnb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rupom.project.airbnb.dto.PaymentResponse;
import dev.rupom.project.airbnb.entity.enums.PaymentMethod;
import dev.rupom.project.airbnb.entity.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentIdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getCachedPayment_whenKeyExists_returnsDeserializedResponse() throws Exception {
        String json = "{\"id\":1,\"transactionId\":\"TXN-123\"}";
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionId("TXN-123")
                .status(PaymentStatus.CONFIRMED)
                .price(BigDecimal.valueOf(250.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(valueOperations.get("idempotency:payment:1:key-abc")).thenReturn(json);
        when(objectMapper.readValue(json, PaymentResponse.class)).thenReturn(response);

        Optional<PaymentResponse> result = idempotencyService.getCachedPayment(1L, "key-abc");

        assertTrue(result.isPresent());
        assertEquals("TXN-123", result.get().getTransactionId());
    }

    @Test
    void getCachedPayment_whenBlankOrNull_returnsEmpty() {
        assertTrue(idempotencyService.getCachedPayment(1L, null).isEmpty());
        assertTrue(idempotencyService.getCachedPayment(1L, "   ").isEmpty());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void getCachedPayment_whenRedisFails_gracefullyReturnsEmpty() {
        when(valueOperations.get(any())).thenThrow(new RedisConnectionFailureException("Offline"));

        Optional<PaymentResponse> result = idempotencyService.getCachedPayment(1L, "key-abc");

        assertTrue(result.isEmpty());
    }

    @Test
    void cachePayment_success_writesToRedis() throws JsonProcessingException {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionId("TXN-123")
                .build();
        String json = "{\"id\":1,\"transactionId\":\"TXN-123\"}";

        when(objectMapper.writeValueAsString(response)).thenReturn(json);

        assertDoesNotThrow(() -> idempotencyService.cachePayment(1L, "key-abc", response));

        verify(valueOperations, times(1)).set(
                eq("idempotency:payment:1:key-abc"),
                eq(json),
                eq(Duration.ofHours(24))
        );
    }

    @Test
    void cachePayment_redisFails_gracefullyHandlesException() throws JsonProcessingException {
        PaymentResponse response = PaymentResponse.builder().id(1L).build();
        when(objectMapper.writeValueAsString(response)).thenReturn("{}");
        doThrow(new RedisConnectionFailureException("Offline")).when(valueOperations).set(any(), any(), any());

        assertDoesNotThrow(() -> idempotencyService.cachePayment(1L, "key-abc", response));
    }
}
