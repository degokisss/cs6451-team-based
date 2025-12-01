package com.example.hotelreservationsystem.base.payment.decorator;

import com.example.hotelreservationsystem.base.payment.PaymentStrategy;
import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.enums.PaymentType;


public abstract class PaymentStrategyDecorator implements PaymentStrategy {

    protected final PaymentStrategy wrappedStrategy;
    public PaymentStrategyDecorator(PaymentStrategy wrappedStrategy) {
        this.wrappedStrategy = wrappedStrategy;
    }


    @Override
    public PaymentResponse pay(PaymentRequest request) {
        return wrappedStrategy.pay(request);
    }

    @Override
    public PaymentType getType() {
        return wrappedStrategy.getType();
    }
}