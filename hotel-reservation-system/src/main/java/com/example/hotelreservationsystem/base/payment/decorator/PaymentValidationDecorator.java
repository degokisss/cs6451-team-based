package com.example.hotelreservationsystem.base.payment.decorator;

import com.example.hotelreservationsystem.base.payment.PaymentStrategy;
import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentValidationDecorator extends PaymentStrategyDecorator {

    public PaymentValidationDecorator(PaymentStrategy wrappedStrategy) {
        super(wrappedStrategy);
    }

    @Override
    public PaymentResponse pay(PaymentRequest request) {
        log.info("Validating request...");

        // Mock validation logic
        if (request.getOrderId() == null) {
            log.error("Validation failed: Order ID missing");
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        log.info("Validation passed.");
        return super.pay(request);
    }
}