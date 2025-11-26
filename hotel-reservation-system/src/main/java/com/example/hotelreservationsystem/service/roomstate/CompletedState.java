package com.example.hotelreservationsystem.service.roomstate;

/**
 * Terminal completed state; transitions are no-ops.
 */
public class CompletedState implements ReservationState {

    @Override
    public void confirm(ReservationContext context) {
        // Already completed
    }

    @Override
    public void cancel(ReservationContext context) {
        // Cannot cancel a completed order
    }

    @Override
    public void complete(ReservationContext context) {
        // Already completed
    }
}
