package com.example.hotelreservationsystem.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event representing a change in booking lock state
 * Used with Observer pattern for lock lifecycle notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockEvent {

    private LockEventType eventType;
    private Long roomId;
    private Long customerId;
    private String lockId;
    private LocalDateTime timestamp;
    private String reason; // For releases: "manual", "expired", "booking_completed"

    public enum LockEventType {
        LOCK_CREATED,
        LOCK_RELEASED,
        LOCK_EXPIRED,
        LOCK_CONFLICT_DETECTED
    }
}
