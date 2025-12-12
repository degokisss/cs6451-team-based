package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.events.LockEvent;
import com.example.hotelreservationsystem.events.LockEventObserver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Booking Lock Service with Observer Pattern support
 * Manages Redis-based room locks and notifies observers of state changes
 */
@Service
@Slf4j
public class BookingLockService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Observer pattern: List of observers to notify on lock events
    private final List<LockEventObserver> observers = new CopyOnWriteArrayList<>();

    private static final String LOCK_PREFIX = "booking:lock:";
    private static final String ROOM_ID_KEY = "roomId";
    private static final String CUSTOMER_ID_KEY = "customerId";
    private static final String LOCK_ID_KEY = "lockId";

    @Value("${booking.lock.ttl-minutes:10}")
    private Integer defaultTtlMinutes;

    /**
     * Constructor that auto-registers Spring-managed observers
     */
    public BookingLockService(
        RedisTemplate<String, String> redisTemplate,
        ObjectMapper objectMapper,
        List<LockEventObserver> observers
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        // Auto-register all Spring-managed observers
        observers.forEach(this::registerObserver);
        log.info("BookingLockService initialized with {} observers", this.observers.size());
    }

    /**
     * Register an observer to receive lock event notifications
     */
    public void registerObserver(LockEventObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            log.debug("Registered observer: {}", observer.getObserverName());
        }
    }

    /**
     * Unregister an observer
     */
    public void unregisterObserver(LockEventObserver observer) {
        observers.remove(observer);
        log.debug("Unregistered observer: {}", observer.getObserverName());
    }

    /**
     * Notify all observers of a lock event
     */
    private void notifyObservers(LockEvent event) {
        log.debug("Notifying {} observers of event: {}", observers.size(), event.getEventType());
        observers.forEach(observer -> {
            try {
                observer.onLockEvent(event);
            } catch (Exception e) {
                log.error("Observer {} failed to handle event", observer.getObserverName(), e);
            }
        });
    }

    /**
     * Create a booking lock for a room
     *
     * @param roomId     The room ID to lock
     * @param customerId The customer ID creating the lock
     * @return The lock ID (UUID)
     * @throws IllegalStateException if room is already locked
     */
    public String createLock(Long roomId, Long customerId) {
        return createLock(roomId, customerId, defaultTtlMinutes);
    }

    /**
     * Create a booking lock for a room with custom TTL
     *
     * @param roomId      The room ID to lock
     * @param customerId  The customer ID creating the lock
     * @param ttlMinutes  Time to live in minutes
     * @return The lock ID (UUID)
     * @throws IllegalStateException if room is already locked
     */
    public String createLock(Long roomId, Long customerId, Integer ttlMinutes) {
        try {
            var key = LOCK_PREFIX + roomId;

            // Check if room is already locked
            if (redisTemplate.hasKey(key)) {
                log.warn("Room {} is already locked", roomId);

                // Notify observers of conflict
                notifyObservers(LockEvent.builder()
                                         .eventType(LockEvent.LockEventType.LOCK_CONFLICT_DETECTED)
                                         .roomId(roomId)
                                         .customerId(customerId)
                                         .timestamp(LocalDateTime.now())
                                         .reason("Room already locked by another user")
                                         .build());

                throw new IllegalStateException("Room is already locked");
            }

            // Generate lock ID
            var lockId = UUID.randomUUID().toString();
            var now = LocalDateTime.now();

            // Create lock data
            Map<String, Object> lockData = new HashMap<>();
            lockData.put(LOCK_ID_KEY, lockId);
            lockData.put(CUSTOMER_ID_KEY, customerId);
            lockData.put(ROOM_ID_KEY, roomId);
            lockData.put("timestamp", now.toString());

            // Store in Redis with TTL
            var lockJson = objectMapper.writeValueAsString(lockData);
            redisTemplate.opsForValue().set(key, lockJson, ttlMinutes, TimeUnit.MINUTES);

            log.info("Created lock {} for room {} by customer {} with TTL {}min", lockId, roomId, customerId, ttlMinutes);

            // Notify observers of lock creation
            notifyObservers(LockEvent.builder()
                                     .eventType(LockEvent.LockEventType.LOCK_CREATED)
                                     .roomId(roomId)
                                     .customerId(customerId)
                                     .lockId(lockId)
                                     .timestamp(now)
                                     .build());

            return lockId;

        } catch (IllegalStateException e) {
            // Room already locked - propagate without wrapping
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize lock data for room: {}", roomId, e);
            throw new RuntimeException("Failed to create booking lock", e);
        } catch (Exception e) {
            log.error("Failed to create lock in Redis for room: {}", roomId, e);
            throw new RuntimeException("Failed to create booking lock", e);
        }
    }

    /**
     * Check if a room is currently locked
     *
     * @param roomId The room ID to check
     * @return true if locked, false otherwise
     */
    public boolean isLocked(Long roomId) {
        try {
            var key = LOCK_PREFIX + roomId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check lock status for room: {}", roomId, e);
            return false;
        }
    }

    /**
     * Get lock information for a room
     *
     * @param roomId The room ID
     * @return Lock data map, or null if not locked
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getLockInfo(Long roomId) {
        try {
            var key = LOCK_PREFIX + roomId;
            var lockJson = redisTemplate.opsForValue().get(key);

            if (lockJson == null) {
                return null;
            }

            return objectMapper.readValue(lockJson, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize lock data for room: {}", roomId, e);
            return null;
        } catch (Exception e) {
            log.error("Failed to get lock info for room: {}", roomId, e);
            return null;
        }
    }

    /**
     * Release a booking lock
     *
     * @param lockId     The lock ID to release
     * @param customerId The customer ID requesting release
     * @return true if released successfully, false if lock doesn't exist or customer mismatch
     */
    public boolean releaseLock(String lockId, Long customerId) {
        try {
            // Find the lock by lockId (need to scan all locks)
            var keys = redisTemplate.keys(LOCK_PREFIX + "*");

            if (keys.isEmpty()) {
                log.warn("No locks found to release for lockId: {}", lockId);
                return false;
            }

            for (var key : keys) {
                Optional<Boolean> result = releaseLockIfMatch(key, lockId, customerId);
                if (result.isPresent()) {
                    return result.get();
                }
            }

            log.warn("Lock not found: {}", lockId);
            return false;

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize lock data for lockId: {}", lockId, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to release lock: {}", lockId, e);
            throw new RuntimeException("Failed to release booking lock", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<Boolean> releaseLockIfMatch(String key, String lockId, Long customerId) throws JsonProcessingException {
        var lockJson = redisTemplate.opsForValue().get(key);
        if (lockJson == null) {
            return Optional.empty();
        }

        Map<String, Object> lockData = objectMapper.readValue(lockJson, Map.class);

        if (!lockId.equals(lockData.get(LOCK_ID_KEY))) {
            return Optional.empty();
        }

        Long lockCustomerId = ((Number) lockData.get(CUSTOMER_ID_KEY)).longValue();
        if (!lockCustomerId.equals(customerId)) {
            log.warn("Customer {} attempted to release lock {} owned by customer {}",
                customerId, lockId, lockCustomerId);
            return Optional.of(false);
        }

        boolean deleted = Boolean.TRUE.equals(redisTemplate.delete(key));
        log.info("Released lock {} for room {} by customer {}",
            lockId, lockData.get(ROOM_ID_KEY), customerId);

        if (deleted) {
            notifyObservers(LockEvent.builder()
                                     .eventType(LockEvent.LockEventType.LOCK_RELEASED)
                                     .roomId(((Number) lockData.get(ROOM_ID_KEY)).longValue())
                                     .customerId(customerId)
                                     .lockId(lockId)
                                     .timestamp(LocalDateTime.now())
                                     .reason("manual")
                                     .build());
        }

        return Optional.of(deleted);
    }

    /**
     * Release a booking lock by room ID (for admin/cleanup purposes)
     *
     * @param roomId The room ID
     * @return true if released successfully
     */
    public boolean releaseLockByRoomId(Long roomId) {
        try {
            var key = LOCK_PREFIX + roomId;

            // Get lock info before deleting for event notification
            var lockInfo = getLockInfo(roomId);

            var deleted = redisTemplate.delete(key);
            log.info("Released lock for room {}: {}", roomId, deleted);

            // Notify observers if lock was successfully deleted
            if (deleted && lockInfo != null) {
                notifyObservers(LockEvent.builder()
                                         .eventType(LockEvent.LockEventType.LOCK_RELEASED)
                                         .roomId(roomId)
                                         .customerId(lockInfo.get(CUSTOMER_ID_KEY) != null ?
                                             ((Number) lockInfo.get(CUSTOMER_ID_KEY)).longValue() : null)
                                         .lockId((String) lockInfo.get(LOCK_ID_KEY))
                                         .timestamp(LocalDateTime.now())
                                         .reason("admin_release")
                                         .build());
            }

            return deleted;
        } catch (Exception e) {
            log.error("Failed to release lock for room: {}", roomId, e);
            throw new RuntimeException("Failed to release booking lock", e);
        }
    }

    /**
     * Get TTL (remaining time) for a room lock in seconds
     *
     * @param roomId The room ID
     * @return TTL in seconds, or -1 if not locked or error
     */
    public Long getLockTtl(Long roomId) {
        try {
            var key = LOCK_PREFIX + roomId;
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to get TTL for room: {}", roomId, e);
            return -1L;
        }
    }
}
