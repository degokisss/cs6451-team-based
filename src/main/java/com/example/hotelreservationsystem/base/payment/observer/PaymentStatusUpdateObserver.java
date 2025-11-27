package com.example.hotelreservationsystem.base.payment.observer;

import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.repository.OrderRepository;
import com.example.hotelreservationsystem.service.roomstate.ReservationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * [Observer Pattern]
 * Concrete Observer #2 ：update order status after payment success
 * Job: After successful payment, change order status to CONFIRMED
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentStatusUpdateObserver implements PaymentObserver {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void onPaymentSuccess(Long orderId, PaymentResponse response) {
        log.info("Payment success for Order ID: {}. Updating status to CONFIRMED...", orderId);

        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            log.error("Order {} not found. Cannot update status.", orderId);
            return;
        }

        Order order = orderOpt.get();

        try {
        // Update PENDING orders only
        ReservationContext context = new ReservationContext(order);
        context.confirm();
            orderRepository.save(order);
            log.info("Order {} is now CONFIRMED.", orderId);
        } catch (Exception e) {
        log.error("Failed to confirm order {} ", orderId);
        }
    }
}