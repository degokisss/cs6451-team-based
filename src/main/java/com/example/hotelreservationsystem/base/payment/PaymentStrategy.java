package com.example.hotelreservationsystem.base.payment;

import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;

/**
 * Strategy Interface for Payment.
 * This implements the Strategy Pattern to allow dynamic selection
 * of payment algorithms (Credit Card, PayPal) at runtime.
 */
public interface PaymentStrategy {
    /**
     * Process the payment request.
     * @param request payment details including order ID and amount
     * @return PaymentResponse with transaction status and ID
     */
    PaymentResponse pay(PaymentRequest request);
}