package com.example.hotelreservationsystem.base.payment;

import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Concrete Strategy for PayPal Payment.
 * Simulates redirection and callback handling for PayPal.
 */
@Service
@Slf4j
public class PayPalPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResponse pay(PaymentRequest request) {
        log.info("PayPal Redirecting to PayPal gateway for Order ID: {}", request.getOrderId());

        // Mock: Simulate user login and approval latency
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String transactionId = "paypal_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("PayPal Payment Verified. TxID: {}", transactionId);

        return PaymentResponse.builder()
                .orderId(request.getOrderId())
                .status("SUCCESS")
                .transactionId(transactionId)
                .message("PayPal payment processed successfully.")
                .build();
    }
}