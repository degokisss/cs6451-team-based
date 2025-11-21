package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.events.LockEvent;
import com.example.hotelreservationsystem.events.LockEventObserver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test demonstrating the Observer pattern in BookingLockService
 * Shows how observers are notified of lock events
 */
@ExtendWith(MockitoExtension.class)
class BookingLockObserverIntegrationTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LockEventObserver mockObserver1;

    @Mock
    private LockEventObserver mockObserver2;

    private BookingLockService bookingLockService;

    @BeforeEach
    void setUp() {
        List<LockEventObserver> observers = new ArrayList<>();
        observers.add(mockObserver1);
        observers.add(mockObserver2);

        bookingLockService = new BookingLockService(
            redisTemplate,
            objectMapper,
            observers
        );

        ReflectionTestUtils.setField(bookingLockService, "defaultTtlMinutes", 10);
    }

    @Test
    void shouldNotifyObserversWhenLockCreated() throws Exception {
        // Arrange
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        bookingLockService.createLock(1L, 100L);

        // Assert - verify both observers were notified
        ArgumentCaptor<LockEvent> eventCaptor1 = ArgumentCaptor.forClass(LockEvent.class);
        ArgumentCaptor<LockEvent> eventCaptor2 = ArgumentCaptor.forClass(LockEvent.class);

        verify(mockObserver1).onLockEvent(eventCaptor1.capture());
        verify(mockObserver2).onLockEvent(eventCaptor2.capture());

        LockEvent event = eventCaptor1.getValue();
        assertEquals(LockEvent.LockEventType.LOCK_CREATED, event.getEventType());
        assertEquals(1L, event.getRoomId());
        assertEquals(100L, event.getCustomerId());
        assertNotNull(event.getLockId());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void shouldNotifyObserversWhenLockConflictDetected() {
        // Arrange - room already locked
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            bookingLockService.createLock(1L, 100L)
        );

        // Verify observers were notified of conflict
        ArgumentCaptor<LockEvent> eventCaptor = ArgumentCaptor.forClass(LockEvent.class);
        verify(mockObserver1).onLockEvent(eventCaptor.capture());

        LockEvent event = eventCaptor.getValue();
        assertEquals(LockEvent.LockEventType.LOCK_CONFLICT_DETECTED, event.getEventType());
        assertEquals(1L, event.getRoomId());
        assertEquals(100L, event.getCustomerId());
        assertEquals("Room already locked by another user", event.getReason());
    }

    @Test
    void shouldNotifyObserversWhenLockReleased() throws Exception {
        // Arrange
        String lockJson = "{\"lockId\":\"test-lock\",\"customerId\":100,\"roomId\":1}";
        when(redisTemplate.keys("booking:lock:*")).thenReturn(java.util.Set.of("booking:lock:1"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("booking:lock:1")).thenReturn(lockJson);
        when(objectMapper.readValue(eq(lockJson), eq(java.util.Map.class))).thenReturn(
            java.util.Map.of("lockId", "test-lock", "customerId", 100, "roomId", 1)
        );
        when(redisTemplate.delete("booking:lock:1")).thenReturn(true);

        // Act
        boolean released = bookingLockService.releaseLock("test-lock", 100L);

        // Assert
        assertTrue(released);

        ArgumentCaptor<LockEvent> eventCaptor = ArgumentCaptor.forClass(LockEvent.class);
        verify(mockObserver1).onLockEvent(eventCaptor.capture());

        LockEvent event = eventCaptor.getValue();
        assertEquals(LockEvent.LockEventType.LOCK_RELEASED, event.getEventType());
        assertEquals(1L, event.getRoomId());
        assertEquals(100L, event.getCustomerId());
        assertEquals("test-lock", event.getLockId());
        assertEquals("manual", event.getReason());
    }

    @Test
    void shouldRegisterAndUnregisterObservers() {
        // Arrange
        LockEventObserver newObserver = mock(LockEventObserver.class);

        // Act - register
        bookingLockService.registerObserver(newObserver);

        // Try to create a lock (will fail but should notify all 3 observers)
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        assertThrows(IllegalStateException.class, () ->
            bookingLockService.createLock(1L, 100L)
        );

        verify(newObserver, times(1)).onLockEvent(any(LockEvent.class));

        // Act - unregister
        bookingLockService.unregisterObserver(newObserver);
        reset(newObserver);

        // Try again - newObserver should not be notified
        assertThrows(IllegalStateException.class, () ->
            bookingLockService.createLock(2L, 200L)
        );

        verify(newObserver, never()).onLockEvent(any(LockEvent.class));
    }

    @Test
    void shouldHandleObserverExceptionsGracefully() throws Exception {
        // Arrange - one observer throws exception
        when(mockObserver1.getObserverName()).thenReturn("FailingObserver");
        doThrow(new RuntimeException("Observer failed"))
            .when(mockObserver1).onLockEvent(any(LockEvent.class));

        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act - lock creation should succeed despite observer failure
        String lockId = bookingLockService.createLock(1L, 100L);

        // Assert
        assertNotNull(lockId);

        // Observer 1 was called but failed
        verify(mockObserver1).onLockEvent(any(LockEvent.class));

        // Observer 2 should still be notified
        verify(mockObserver2).onLockEvent(any(LockEvent.class));
    }
}
