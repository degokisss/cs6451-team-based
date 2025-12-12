package com.example.hotelreservationsystem.service.roomstate;

import lombok.extern.slf4j.Slf4j;

/**
 * Terminal completed state; invalid transitions throw exceptions.
 */
@Slf4j
public class CompletedState implements ReservationState {

    @Override
    public void confirm(ReservationContext context) {
        log.error("Order already COMPLETED");
        throw new UnsupportedOperationException("Order already completed");
    }

    @Override
    public void cancel(ReservationContext context) {
        log.error("Cannot cancel order in COMPLETED state");
        throw new UnsupportedOperationException("Completed orders cannot be cancelled");
    }

    @Override
    public void complete(ReservationContext context) {
        log.error("Order already COMPLETED");
        throw new UnsupportedOperationException("Order already completed");
    }
}
