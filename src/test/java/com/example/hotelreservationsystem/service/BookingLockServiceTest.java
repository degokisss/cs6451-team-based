package com.example.hotelreservationsystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingLockServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    private BookingLockService bookingLockService;

    private static final Long TEST_ROOM_ID = 1L;
    private static final Long TEST_CUSTOMER_ID = 100L;
    private static final Integer TEST_TTL_MINUTES = 10;

    @BeforeEach
    void setUp() {
        // Create service instance with empty observer list
        bookingLockService = new BookingLockService(
            redisTemplate,
            objectMapper,
            Collections.emptyList()  // No observers for unit tests
        );

        // Set default TTL
        ReflectionTestUtils.setField(bookingLockService, "defaultTtlMinutes", TEST_TTL_MINUTES);
    }

    @Test
    void shouldCreateLockSuccessfully() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{\"lockId\":\"test-lock\"}");
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        var lockId = bookingLockService.createLock(TEST_ROOM_ID, TEST_CUSTOMER_ID);

        assertNotNull(lockId);
        verify(redisTemplate).hasKey("booking:lock:" + TEST_ROOM_ID);
        verify(valueOperations).set(eq("booking:lock:" + TEST_ROOM_ID), anyString(), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void shouldThrowExceptionWhenRoomAlreadyLocked() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        var exception = assertThrows(IllegalStateException.class, () ->
            bookingLockService.createLock(TEST_ROOM_ID, TEST_CUSTOMER_ID)
        );

        assertEquals("Room is already locked", exception.getMessage());
        verify(redisTemplate).hasKey("booking:lock:" + TEST_ROOM_ID);
        verifyNoInteractions(valueOperations);
    }

    @Test
    void shouldCheckIfRoomIsLocked() {
        when(redisTemplate.hasKey("booking:lock:" + TEST_ROOM_ID)).thenReturn(true);

        var isLocked = bookingLockService.isLocked(TEST_ROOM_ID);

        assertTrue(isLocked);
        verify(redisTemplate).hasKey("booking:lock:" + TEST_ROOM_ID);
    }

    @Test
    void shouldReturnFalseWhenRoomIsNotLocked() {
        when(redisTemplate.hasKey("booking:lock:" + TEST_ROOM_ID)).thenReturn(false);

        var isLocked = bookingLockService.isLocked(TEST_ROOM_ID);

        assertFalse(isLocked);
    }

    @Test
    void shouldReleaseLockSuccessfully() throws Exception {
        var lockId = "test-lock-id";
        var lockJson = "{\"lockId\":\"test-lock-id\",\"customerId\":100,\"roomId\":1}";
        var lockData = Map.of(
            "lockId", lockId,
            "customerId", 100,
            "roomId", 1
        );

        when(redisTemplate.keys("booking:lock:*")).thenReturn(Set.of("booking:lock:1"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("booking:lock:1")).thenReturn(lockJson);
        when(objectMapper.readValue(eq(lockJson), eq(Map.class))).thenReturn(lockData);
        when(redisTemplate.delete("booking:lock:1")).thenReturn(true);

        var released = bookingLockService.releaseLock(lockId, TEST_CUSTOMER_ID);

        assertTrue(released);
        verify(redisTemplate).delete("booking:lock:1");
    }

    @Test
    void shouldReturnFalseWhenReleasingNonExistentLock() {
        when(redisTemplate.keys("booking:lock:*")).thenReturn(Set.of());

        var released = bookingLockService.releaseLock("non-existent", TEST_CUSTOMER_ID);

        assertFalse(released);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void shouldReturnFalseWhenCustomerDoesNotOwnLock() throws Exception {
        var lockId = "test-lock-id";
        var lockJson = "{\"lockId\":\"test-lock-id\",\"customerId\":999,\"roomId\":1}";
        var lockData = Map.of(
            "lockId", lockId,
            "customerId", 999,  // Different customer
            "roomId", 1
        );

        when(redisTemplate.keys("booking:lock:*")).thenReturn(Set.of("booking:lock:1"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("booking:lock:1")).thenReturn(lockJson);
        when(objectMapper.readValue(eq(lockJson), eq(Map.class))).thenReturn(lockData);

        var released = bookingLockService.releaseLock(lockId, TEST_CUSTOMER_ID);

        assertFalse(released);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void shouldReleaseLockByRoomId() {
        when(redisTemplate.delete("booking:lock:" + TEST_ROOM_ID)).thenReturn(true);

        var released = bookingLockService.releaseLockByRoomId(TEST_ROOM_ID);

        assertTrue(released);
        verify(redisTemplate).delete("booking:lock:" + TEST_ROOM_ID);
    }

    @Test
    void shouldGetLockTtl() {
        when(redisTemplate.getExpire("booking:lock:" + TEST_ROOM_ID, TimeUnit.SECONDS)).thenReturn(300L);

        var ttl = bookingLockService.getLockTtl(TEST_ROOM_ID);

        assertEquals(300L, ttl);
        verify(redisTemplate).getExpire("booking:lock:" + TEST_ROOM_ID, TimeUnit.SECONDS);
    }

    @Test
    void shouldGetLockInfo() throws Exception {
        var lockJson = "{\"lockId\":\"test-lock\",\"customerId\":100,\"roomId\":1}";
        var lockData = Map.of(
            "lockId", "test-lock",
            "customerId", 100,
            "roomId", 1
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("booking:lock:" + TEST_ROOM_ID)).thenReturn(lockJson);
        when(objectMapper.readValue(eq(lockJson), eq(Map.class))).thenReturn(lockData);

        var info = bookingLockService.getLockInfo(TEST_ROOM_ID);

        assertNotNull(info);
        assertEquals("test-lock", info.get("lockId"));
        assertEquals(100, info.get("customerId"));
    }

    @Test
    void shouldReturnNullWhenGettingInfoForNonExistentLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("booking:lock:" + TEST_ROOM_ID)).thenReturn(null);

        var info = bookingLockService.getLockInfo(TEST_ROOM_ID);

        assertNull(info);
    }
}
