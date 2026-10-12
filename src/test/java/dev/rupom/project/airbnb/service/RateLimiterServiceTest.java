package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void checkRateLimit_underLimit_allowsRequest() {
        when(valueOperations.increment("test-key")).thenReturn(1L);

        assertDoesNotThrow(() ->
                rateLimiterService.checkRateLimit("test-key", 10, 60, "Limit exceeded")
        );

        verify(stringRedisTemplate, times(1)).expire(eq("test-key"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void checkRateLimit_exceededLimit_throwsRateLimitExceededException() {
        when(valueOperations.increment("test-key")).thenReturn(11L);

        assertThrows(RateLimitExceededException.class, () ->
                rateLimiterService.checkRateLimit("test-key", 10, 60, "Too many requests")
        );
    }

    @Test
    void checkRateLimit_redisOffline_gracefullyPermitsRequest() {
        when(valueOperations.increment("test-key")).thenThrow(new RedisConnectionFailureException("Connection refused"));

        assertDoesNotThrow(() ->
                rateLimiterService.checkRateLimit("test-key", 10, 60, "Too many requests")
        );
    }
}
