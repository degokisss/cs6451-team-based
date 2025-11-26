package com.example.hotelreservationsystem.service.roomstate;

import com.example.hotelreservationsystem.enums.OrderStatus;

/**
 * Pending → Confirmed/Cancelled transitions.
 */
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
        // Not allowed; keep pending
    }
}
