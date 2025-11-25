package com.example.hotelreservationsystem.base.payment;

import com.example.hotelreservationsystem.base.payment.decorator.PaymentRetryDecorator;
import com.example.hotelreservationsystem.base.payment.decorator.PaymentValidationDecorator;
import com.example.hotelreservationsystem.enums.PaymentType;
import org.springframework.stereotype.Component;

@Component
public class PaymentFactory {

    public PaymentStrategy getStrategy(PaymentType paymentType) {

         PaymentStrategy rawStrategy;

        // 1. Instantiate the base strategy
        switch (paymentType) {
            case CREDIT_CARD:
                rawStrategy = new CreditCardPaymentStrategy();
                break;
            case PAYPAL:
                rawStrategy = new PayPalPaymentStrategy();
                break;
            default:
                throw new IllegalArgumentException("Invalid payment type");
        }

        // 2. Assemble decorators

        // First wrap with validation
        PaymentStrategy validatedStrategy = new PaymentValidationDecorator(rawStrategy);

        // Then wrap with logging
        PaymentStrategy finalStrategy = new PaymentRetryDecorator(validatedStrategy);

        return finalStrategy;
    }
}