package com.example.hotelreservationsystem.base.payment;

import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.enums.PaymentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Concrete detail for credit card strategy.
 * Simulates interaction with card providers.
 */
@Service
@Slf4j
public class CreditCardPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResponse pay(PaymentRequest request) {
        log.info("CreditCard Processing payment for Order ID: {}", request.getOrderId());

        // Mock: Simulate network latency
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Mock: Generate a transaction ID
        String transactionId = "creditcard_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("CreditCard Payment Successful. TxID: {}", transactionId);

        return PaymentResponse.builder()
                .orderId(request.getOrderId())
                .status("SUCCESS")
                .transactionId(transactionId)
                .message("Credit Card payment processed successfully.")
                .build();
    }

    //return its type to factory
    @Override
    public PaymentType getType() {
        return PaymentType.CREDIT_CARD; // 我是信用卡策略
    }
}