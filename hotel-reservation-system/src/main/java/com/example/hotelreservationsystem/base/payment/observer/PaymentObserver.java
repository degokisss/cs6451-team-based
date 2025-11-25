package com.example.hotelreservationsystem.base.payment.observer;

import com.example.hotelreservationsystem.dto.PaymentResponse;

/**
 * [Observer Pattern]
 * Observer.
 */
public interface PaymentObserver {
    void onPaymentSuccess(Long orderId, PaymentResponse response);
}