package com.example.hotelreservationsystem.base.payment.decorator;

import com.example.hotelreservationsystem.base.payment.PaymentStrategy;
import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentRetryDecorator extends PaymentStrategyDecorator {

    private static final int MAX_RETRIES = 3;

    public PaymentRetryDecorator(PaymentStrategy wrappedStrategy) {
        super(wrappedStrategy);
    }

    @Override
    public PaymentResponse pay(PaymentRequest request) {
        int attempts = 0;
        PaymentResponse response = null;

        // Retry loop mechanism
        while (attempts < MAX_RETRIES) {
            attempts++;
            try {
                log.info("Attempt {}/{} for Order: {}",
                        attempts, MAX_RETRIES, request.getOrderId());

                // Call the next layer
                response = super.pay(request);

                // If successful, return immediately
                if ("SUCCESS".equals(response.getStatus())) {
                    return response;
                }

                // If FAILED and not the last attempt, log warning and continue loop
                log.warn("Payment failed on attempt {}. Retrying...", attempts);

            } catch (Exception e) {
                log.error("Exception on attempt {}: {}", attempts, e.getMessage());
            }
        }

        // If all 3 attempts failed
        log.error("All {} attempts failed.", MAX_RETRIES);
        return response; // Return the last failure response
    }
}