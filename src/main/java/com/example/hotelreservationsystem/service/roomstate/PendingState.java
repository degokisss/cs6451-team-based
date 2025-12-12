package com.example.hotelreservationsystem.service.roomstate;

import com.example.hotelreservationsystem.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Pending → Confirmed/Cancelled transitions.
 */
@Slf4j
public class PendingState implements ReservationState {

    @Override
    public void confirm(ReservationContext context) {
        context.transitionTo(OrderStatus.CONFIRMED);
    }

    @Override
    public void cancel(ReservationContext context) {
        context.transitionTo(OrderStatus.CANCELLED);
    }

    @Override
    public void complete(ReservationContext context) {
        log.error("Cannot complete order while in PENDING state");
        throw new UnsupportedOperationException("Order is pending and cannot be completed");
    }
}
