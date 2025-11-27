package com.example.hotelreservationsystem.service.roomstate;

import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.enums.OrderStatus;

/**
 * Lightweight context that delegates status changes to reservation states.
 * Keeps OrderStatus as the single source of truth while using state classes for transitions.
 */
public class ReservationContext {

    private final Order order;

    public ReservationContext(Order order) {
        this.order = order;
    }

    public void confirm() {
        getState(order.getOrderStatus()).confirm(this);
    }

    public void cancel() {
        getState(order.getOrderStatus()).cancel(this);
    }

    public void complete() {
        getState(order.getOrderStatus()).complete(this);
    }

    void transitionTo(OrderStatus status) {
        order.setOrderStatus(status);
    }

    public Order getOrder() {
        return order;
    }

    private ReservationState getState(OrderStatus status) {
        return switch (status) {
            case PENDING -> new PendingState();
            case CONFIRMED -> new ConfirmedState();
            case CANCELLED -> new CancelledState();
            case COMPLETED -> new CompletedState();
        };
    }
}
