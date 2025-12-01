package com.example.hotelreservationsystem.base.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Concrete implementation of a credit card payment provider.
 * This class simulates the process of handling payments made via credit card.
 * Note: The 'Order' entity is expected to be provided from another context.
 */
@Service
@Slf4j
public class CreditCardPaymentProvider implements PaymentProvider {
    @Override
    public boolean processPayment(Long orderId, Double amount) {
        // Simulate credit card payment processing logic.
        log.info("Processing Credit Card payment for order {} of amount {}", orderId, amount);
        // Assume payment processing always succeeds for this simulation.
        return true;
    }
}
