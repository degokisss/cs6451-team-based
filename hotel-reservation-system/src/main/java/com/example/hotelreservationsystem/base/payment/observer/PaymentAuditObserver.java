package com.example.hotelreservationsystem.base.payment.observer;

import com.example.hotelreservationsystem.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * [Observer Pattern]
 * Concrete Observer #1
 **/
@Component
@Slf4j
public class PaymentAuditObserver implements PaymentObserver {
    @Override
    public void onPaymentSuccess(Long orderId, PaymentResponse response) {
        log.info("logging transaction {} to secure notebook(MOCK).", response.getTransactionId());
    }
}