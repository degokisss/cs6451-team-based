package com.example.hotelreservationsystem.service.roomstate;

import lombok.extern.slf4j.Slf4j;

/**
 * Terminal cancelled state; invalid transitions throw exceptions.
 */
@Slf4j
public class CancelledState implements ReservationState {

    @Override
    public void confirm(ReservationContext context) {
        log.error("Cancelled state");
        throw new UnsupportedOperationException("Order cancelled");
    }

    @Override
    public void cancel(ReservationContext context) {
        log.error("Order already CANCELLED");
        throw new UnsupportedOperationException("Order already cancelled");
    }

    @Override
    public void complete(ReservationContext context) {
        log.error("Cannot complete order in CANCELLED state");
        throw new UnsupportedOperationException("Cancelled orders cannot be completed");
    }
}
