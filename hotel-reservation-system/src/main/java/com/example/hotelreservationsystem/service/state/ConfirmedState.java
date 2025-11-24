package com.example.hotelreservationsystem.service.state;

import com.example.hotelreservationsystem.enums.OrderStatus;

/**
 * Confirmed → Completed/Cancelled transitions.
 */
public class ConfirmedState implements ReservationState {

    @Override
    public void confirm(ReservationContext context) {
        // Already confirmed; no change
    }

    @Override
    public void cancel(ReservationContext context) {
        context.transitionTo(OrderStatus.CANCELLED);
    }

    @Override
    public void complete(ReservationContext context) {
        context.transitionTo(OrderStatus.COMPLETED);
    }
}
