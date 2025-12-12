package com.example.hotelreservationsystem.service.roomstate;

import com.example.hotelreservationsystem.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Confirmed → Completed/Cancelled transitions.
 */
@Slf4j
public class ConfirmedState implements ReservationState {

    @Override
    public void confirm(ReservationContext context) {
        log.error("Order already CONFIRMED");
        throw new UnsupportedOperationException("Order already confirmed");
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
