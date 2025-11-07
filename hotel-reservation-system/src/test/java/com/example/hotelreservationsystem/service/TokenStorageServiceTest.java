package com.example.hotelreservationsystem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenStorageServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenStorageService tokenStorageService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_TOKEN = "test-jwt-token";
    private static final Long TEST_EXPIRATION = 1800000L; // 30 minutes

    @BeforeEach
    void setUp() {
        // Setup is done per-test to avoid unnecessary stubbing warnings
    }

    @Test
    void shouldStoreTokenSuccessfully() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        tokenStorageService.storeToken(TEST_EMAIL, TEST_TOKEN, TEST_EXPIRATION);

        verify(valueOperations).set(eq("auth:token:" + TEST_TOKEN), eq(TEST_EMAIL), eq(TEST_EXPIRATION), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void shouldThrowExceptionWhenStoreTokenFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed")).when(valueOperations)
                                                                .set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        assertThrows(RuntimeException.class, () -> tokenStorageService.storeToken(TEST_EMAIL, TEST_TOKEN, TEST_EXPIRATION));
    }

    @Test
    void shouldValidateTokenWhenExists() {
        when(redisTemplate.hasKey("auth:token:" + TEST_TOKEN)).thenReturn(true);

        var result = tokenStorageService.validateToken(TEST_TOKEN);

        assertTrue(result);
        verify(redisTemplate).hasKey("auth:token:" + TEST_TOKEN);
    }

    @Test
    void shouldReturnFalseWhenTokenDoesNotExist() {
        when(redisTemplate.hasKey("auth:token:" + TEST_TOKEN)).thenReturn(false);

        var result = tokenStorageService.validateToken(TEST_TOKEN);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenValidationFails() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));

        var result = tokenStorageService.validateToken(TEST_TOKEN);

        assertFalse(result); // Fail closed on error
    }

    @Test
    void shouldRemoveTokenSuccessfully() {
        when(redisTemplate.delete("auth:token:" + TEST_TOKEN)).thenReturn(true);

        var result = tokenStorageService.removeToken(TEST_TOKEN);

        assertTrue(result);
        verify(redisTemplate).delete("auth:token:" + TEST_TOKEN);
    }

    @Test
    void shouldReturnFalseWhenTokenNotFoundDuringRemoval() {
        when(redisTemplate.delete("auth:token:" + TEST_TOKEN)).thenReturn(false);

        var result = tokenStorageService.removeToken(TEST_TOKEN);

        assertFalse(result);
    }

    @Test
    void shouldThrowExceptionWhenRemoveTokenFails() {
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis error"));

        assertThrows(RuntimeException.class, () -> tokenStorageService.removeToken(TEST_TOKEN));
    }

    @Test
    void shouldGetEmailFromToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:token:" + TEST_TOKEN)).thenReturn(TEST_EMAIL);

        var email = tokenStorageService.getEmailFromToken(TEST_TOKEN);

        assertEquals(TEST_EMAIL, email);
    }

    @Test
    void shouldReturnNullWhenEmailNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:token:" + TEST_TOKEN)).thenReturn(null);

        var email = tokenStorageService.getEmailFromToken(TEST_TOKEN);

        assertNull(email);
    }

    @Test
    void shouldReturnNullWhenGetEmailFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        var email = tokenStorageService.getEmailFromToken(TEST_TOKEN);

        assertNull(email);
    }
}
