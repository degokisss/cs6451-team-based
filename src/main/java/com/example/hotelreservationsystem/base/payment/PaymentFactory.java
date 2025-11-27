package com.example.hotelreservationsystem.base.payment;

import com.example.hotelreservationsystem.base.payment.decorator.PaymentRetryDecorator;
import com.example.hotelreservationsystem.base.payment.decorator.PaymentValidationDecorator;
import com.example.hotelreservationsystem.enums.PaymentType;
import com.example.hotelreservationsystem.repository.OrderRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentFactory {
    private final OrderRepository orderRepository;

    public PaymentStrategy getStrategy(PaymentType paymentType) {

         PaymentStrategy rawStrategy = switch (paymentType) {
             case CREDIT_CARD -> new CreditCardPaymentStrategy();
             case PAYPAL -> new PayPalPaymentStrategy();
             default -> throw new IllegalArgumentException("Invalid payment type");
         };

        // 1. Instantiate the base strategy

        // 2. Assemble the Decorators

        // Inner:  'Retry' logic
        // handles network failures for the payment process.
        PaymentStrategy retryStrategy = new PaymentRetryDecorator(rawStrategy);

        // Outer:  'Validation' logic
        // checks the database first. If validation fails, noneed for retrying.

        return new PaymentValidationDecorator(retryStrategy, orderRepository);

    }
}