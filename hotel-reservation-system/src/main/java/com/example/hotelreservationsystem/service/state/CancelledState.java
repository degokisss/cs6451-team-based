package com.example.hotelreservationsystem.service.state;

/**
 * Terminal cancelled state; transitions are no-ops.
 */
public class CancelledState implements ReservationState {

    @Override
    public void confirm(ReservationContext context) {
        // No transition from cancelled
    }

    @Override
    public void cancel(ReservationContext context) {
        // Already cancelled
    }

    @Override
    public void complete(ReservationContext context) {
        // Cannot complete a cancelled order
    }
}
