package com.example.hotelreservationsystem.enums;

/**
 * Order status enum implementing State Pattern for booking lifecycle
 * State transitions: PENDING → CONFIRMED → COMPLETED / CANCELLED
 */
public enum OrderStatus {
    /**
     * Initial state when order is created from a booking lock
     */
    PENDING,

    /**
     * Order validated and confirmed (payment processed in future stories)
     */
    CONFIRMED,

    /**
     * Order cancelled by customer or system
     */
    CANCELLED,

    /**
     * Order completed after customer check-out
     */
    COMPLETED
}
