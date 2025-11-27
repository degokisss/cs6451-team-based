package com.example.hotelreservationsystem.base.payment;

/**
 * Interface for payment providers.
 * Defines the contract for processing payments. Implementations of this interface
 * will handle the specifics of different payment methods.
 */
public interface PaymentProvider {
    /**
     * Processes a payment for a given order.
     * Note: The 'orderId' parameter corresponds to an 'Order' entity,
     * which is expected to be defined elsewhere.
     *
     * @param orderId The unique identifier of the order.
     * @param amount The payment amount.
     * @return {@code true} if the payment was successful, {@code false} otherwise.
     */
    boolean processPayment(Long orderId, Double amount);
}
