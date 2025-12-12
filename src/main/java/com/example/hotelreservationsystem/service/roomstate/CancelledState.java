package com.example.hotelreservationsystem.service.roomstate;

import com.example.hotelreservationsystem.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Terminal cancelled state; invalid transitions throw exceptions.
 */
@Slf4j
public class CancelledState implements ReservationState {

    @Override
    public void confirm(ReservationContext context) {
        log.warn("Cannot confirm cancelled order - staying in CANCELLED");
        stay(context, OrderStatus.CANCELLED);
    }

    @Override
    public void cancel(ReservationContext context) {
        log.warn("Order already CANCELLED - staying in CANCELLED");
        stay(context, OrderStatus.CANCELLED);
    }

    @Override
    public void complete(ReservationContext context) {
        log.warn("Cannot complete order in CANCELLED state - staying in CANCELLED");
        stay(context, OrderStatus.CANCELLED);
    }
}