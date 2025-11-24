package com.example.hotelreservationsystem.service.state;

import com.example.hotelreservationsystem.enums.OrderStatus;

/**
 * State contract for reservation lifecycle transitions.
 * Implementations should only orchestrate OrderStatus changes; side effects stay in services.
 */
public interface ReservationState {

    void confirm(ReservationContext context);

    void cancel(ReservationContext context);

    void complete(ReservationContext context);

    /**
     * Helper for no-op transitions to keep code explicit.
     */
    default void stay(ReservationContext context, OrderStatus status) {
        context.transitionTo(status);
    }
}
