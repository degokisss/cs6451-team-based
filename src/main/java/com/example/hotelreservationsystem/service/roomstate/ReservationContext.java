package com.example.hotelreservationsystem.service.roomstate;

import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.enums.OrderStatus;
import lombok.Getter;

/**
 * Lightweight context that delegates status changes to reservation states.
 * Keeps OrderStatus as the single source of truth while using state classes for transitions.
 */
@Getter
public class ReservationContext {

    private final Order order;

    public ReservationContext(Order order) {
        this.order = order;
    }

    public void confirm() {
        resolveState().confirm(this);
    }

    public void cancel() {
        resolveState().cancel(this);
    }

    public void complete() {
        resolveState().complete(this);
    }

    void transitionTo(OrderStatus status) {
        order.setOrderStatus(status);
        order.setReservationState(stateFor(status));
    }

    private ReservationState resolveState() {
        // Always resolve state from current OrderStatus to avoid stale state after manual status changes
        ReservationState state = stateFor(order.getOrderStatus());
        order.setReservationState(state);
        return state;
    }

    public static ReservationState stateFor(OrderStatus status) {
        return switch (status) {
            case PENDING -> new PendingState();
            case CONFIRMED -> new ConfirmedState();
            case CANCELLED -> new CancelledState();
            case COMPLETED -> new CompletedState();
        };
    }
}