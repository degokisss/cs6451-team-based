package com.example.hotelreservationsystem.service.roomstate;

import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationContextTest {

    @Test
    void shouldTransitionFromPendingToConfirmed() {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.PENDING);

        new ReservationContext(order).confirm();

        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
    }

    @Test
    void shouldTransitionFromPendingToCancelled() {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.PENDING);

        new ReservationContext(order).cancel();

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
    }

    @Test
    void shouldNoOpCompleteWhenPending() {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.PENDING);

        new ReservationContext(order).complete();

        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test
    void shouldTransitionConfirmedToCompletedOrCancelled() {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.CONFIRMED);
        ReservationContext context = new ReservationContext(order);

        context.complete();
        assertEquals(OrderStatus.COMPLETED, order.getOrderStatus());

        // reset to confirm cancel flow
        order.setOrderStatus(OrderStatus.CONFIRMED);
        context.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
    }

    @Test
    void shouldKeepTerminalStatesAsNoOps() {
        Order cancelled = new Order();
        cancelled.setOrderStatus(OrderStatus.CANCELLED);
        ReservationContext cancelledContext = new ReservationContext(cancelled);

        cancelledContext.confirm();
        cancelledContext.complete();
        assertEquals(OrderStatus.CANCELLED, cancelled.getOrderStatus());

        Order completed = new Order();
        completed.setOrderStatus(OrderStatus.COMPLETED);
        ReservationContext completedContext = new ReservationContext(completed);

        completedContext.confirm();
        completedContext.cancel();
        assertEquals(OrderStatus.COMPLETED, completed.getOrderStatus());
    }
}
