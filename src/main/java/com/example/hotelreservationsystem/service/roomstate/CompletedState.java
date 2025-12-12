package com.example.hotelreservationsystem.service.roomstate;

import com.example.hotelreservationsystem.enums.OrderStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Terminal completed state; invalid transitions throw exceptions.
 */
@Slf4j
public class CompletedState implements ReservationState {

    @Override
    public void confirm(ReservationContext context) {
        log.warn("Order already COMPLETED - staying in COMPLETED");
        stay(context, OrderStatus.COMPLETED);
    }

    @Override
    public void cancel(ReservationContext context) {
        log.warn("Cannot cancel order in COMPLETED state - staying in COMPLETED");
        stay(context, OrderStatus.COMPLETED);
    }

    @Override
    public void complete(ReservationContext context) {
        log.warn("Order already COMPLETED - staying in COMPLETED");
        stay(context, OrderStatus.COMPLETED);
    }
}