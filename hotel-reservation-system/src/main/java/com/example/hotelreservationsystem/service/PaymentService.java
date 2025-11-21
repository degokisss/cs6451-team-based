package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.base.payment.PaymentFactory;
import com.example.hotelreservationsystem.base.payment.PaymentProvider;
import com.example.hotelreservationsystem.enums.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentFactory paymentFactory;

    // --- DEPENDENCIES: Waiting for Booking Module ---
    // private final OrderRepository orderRepository;
    // ------------------------------------------------

    /**
     * Executes a payment for a specific order using the given payment type.
     *
     * @param orderId     The ID of the order to be paid.
     * @param paymentType The method of payment (e.g., CREDIT_CARD, PAYPAL).
     * @return true if the payment was successful, false otherwise.
     */
    @Transactional
    public boolean executePayment(Long orderId, PaymentType paymentType) {
        log.info("Initiating payment for Order ID: {} via {}", orderId, paymentType);

        // --- STEP 1: Retrieve Order Information ---
        // TODO: Replace this mock data with actual database calls once Order entity is ready.
        // Order order = orderRepository.findById(orderId)
        //     .orElseThrow(() -> new RuntimeException("Order not found"));
        // Double amountToPay = order.getTotalPrice();

        // MOCK DATA: Simulating an amount of 150.00 for any order.
        Double amountToPay = 150.00;
        log.debug("MOCK: Retrieved amount to pay for Order ID {}: {}", orderId, amountToPay);

        // --- STEP 2: Get Payment Provider from Factory ---
        // Use the Factory Pattern to get the correct implementation based on the enum.
        PaymentProvider provider = paymentFactory.getProvider(paymentType);

        if (provider == null) {
            log.error("Payment failed: No provider found for payment type {}", paymentType);
            return false;
        }

        // --- STEP 3: Process Payment ---
        // Delegate the actual payment logic to the specific provider (CreditCard, PayPal, etc.).
        boolean paymentSuccess = provider.processPayment(orderId, amountToPay);

        // --- STEP 4: Update Order Status ---
        if (paymentSuccess) {
            log.info("Payment processed successfully for Order ID: {}", orderId);

            // TODO: Update order status to PAID in the database.
            // order.setStatus(OrderStatus.PAID);
            // orderRepository.save(order);

            log.info("MOCK: Order status would be updated to 'PAID' here.");
        } else {
            log.warn("Payment processing failed for Order ID: {}", orderId);

            // TODO: Update order status to PAYMENT_FAILED (optional logic).
            // order.setStatus(OrderStatus.PAYMENT_FAILED);
            // orderRepository.save(order);

            log.info("MOCK: Order status would be updated to 'PAYMENT_FAILED' here.");
        }

        return paymentSuccess;
    }
}