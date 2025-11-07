package com.example.hotelreservationsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenStorageService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_PREFIX = "auth:token:";

    /**
     * Store a token in Redis with the specified expiration time
     *
     * @param email      The user's email (used as value)
     * @param token      The JWT token to store
     * @param expiration Expiration time in milliseconds
     */
    public void storeToken(String email, String token, Long expiration) {
        try {
            var key = TOKEN_PREFIX + token;
            redisTemplate.opsForValue().set(key, email, expiration, TimeUnit.MILLISECONDS);
            log.debug("Stored token for user: {} with expiration: {}ms", email, expiration);
        } catch (Exception e) {
            log.error("Failed to store token in Redis for user: {}", email, e);
            throw new RuntimeException("Failed to store token in Redis", e);
        }
    }

    /**
     * Validate if a token exists in Redis (is not logged out or expired)
     *
     * @param token The JWT token to validate
     * @return true if token exists in Redis, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            var key = TOKEN_PREFIX + token;
            var hasKey = redisTemplate.hasKey(key);
            log.debug("Token validation result: {}", hasKey);
            return hasKey;
        } catch (Exception e) {
            log.error("Failed to validate token in Redis", e);
            return false;
        }
    }

    /**
     * Remove a token from Redis (logout)
     *
     * @param token The JWT token to remove
     * @return true if token was removed, false if it didn't exist
     */
    public boolean removeToken(String token) {
        try {
            var key = TOKEN_PREFIX + token;
            var deleted = redisTemplate.delete(key);
            log.debug("Token removal result: {}", deleted);
            return deleted;
        } catch (Exception e) {
            log.error("Failed to remove token from Redis", e);
            throw new RuntimeException("Failed to remove token from Redis", e);
        }
    }

    /**
     * Get the email associated with a token
     *
     * @param token The JWT token
     * @return The email associated with the token, or null if not found
     */
    public String getEmailFromToken(String token) {
        try {
            var key = TOKEN_PREFIX + token;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get email from token in Redis", e);
            return null;
        }
    }
}
