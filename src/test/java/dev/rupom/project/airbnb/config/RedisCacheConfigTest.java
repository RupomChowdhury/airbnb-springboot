package dev.rupom.project.airbnb.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private Cache cache;

    private RedisCacheConfig redisCacheConfig;

    @BeforeEach
    void setUp() {
        redisCacheConfig = new RedisCacheConfig();
    }

    @Test
    void cacheManager_CreatesManagerSuccessfully() {
        RedisCacheManager manager = redisCacheConfig.cacheManager(connectionFactory);
        assertNotNull(manager);
    }

    @Test
    void errorHandler_GracefullyHandlesCacheExceptionsWithoutThrowing() {
        CacheErrorHandler handler = redisCacheConfig.errorHandler();
        assertNotNull(handler);

        when(cache.getName()).thenReturn("testCache");
        RuntimeException testEx = new RuntimeException("Redis connection refused simulation");

        assertDoesNotThrow(() -> handler.handleCacheGetError(testEx, cache, "key1"));
        assertDoesNotThrow(() -> handler.handleCachePutError(testEx, cache, "key1", "val1"));
        assertDoesNotThrow(() -> handler.handleCacheEvictError(testEx, cache, "key1"));
        assertDoesNotThrow(() -> handler.handleCacheClearError(testEx, cache));
    }
}
