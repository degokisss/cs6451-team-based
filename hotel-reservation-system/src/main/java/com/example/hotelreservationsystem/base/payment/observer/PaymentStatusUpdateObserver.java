package com.example.hotelreservationsystem.base.payment.observer;

import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.enums.OrderStatus;
import com.example.hotelreservationsystem.repository.OrderRepository;
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

        // Update PENDING orders only
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            // Change status to CONFIRMED
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Order {} is now CONFIRMED.", orderId);
        } else {
            log.warn("Order {} is already in state {}, skipping confirmation.", orderId, order.getOrderStatus());
        }
    }
}