package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Checks if a given key has exceeded the allowed number of requests within the duration window.
     *
     * @param key             Redis rate limit key (e.g. ratelimit:auth:login:127.0.0.1)
     * @param maxRequests     Maximum allowed requests in the time window
     * @param durationSeconds Duration of the rate limit window in seconds
     * @param errorMessage    Message to return if rate limit is exceeded
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkRateLimit(String key, int maxRequests, int durationSeconds, String errorMessage) {
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(durationSeconds));
            }

            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded for key [{}]. Request count: {} (limit: {})", key, count, maxRequests);
                throw new RateLimitExceededException(errorMessage);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis rate limiter unavailable for key [{}]: {}. Gracefully permitting request.", key, e.getMessage());
        }
    }
}
