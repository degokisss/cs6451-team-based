package com.example.hotelreservationsystem.base.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Concrete implementation of a PayPal payment provider.
 * This class simulates the process of handling payments made via PayPal.
 * Note: The 'Order' entity is expected to be provided from another context.
 */
@Service
@Slf4j
public class PayPalPaymentProvider implements PaymentProvider {
    @Override
    public boolean processPayment(Long orderId, Double amount) {
        // Simulate PayPal payment processing logic.
        log.info("Processing PayPal payment for order {} of amount {}", orderId, amount);
        // Assume payment processing always succeeds for this simulation.
        return true;
    }
}
