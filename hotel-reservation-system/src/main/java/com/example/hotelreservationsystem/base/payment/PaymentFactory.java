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

        // 2. Assemble the Decorators

        // Inner:  'Retry' logic
        // handles network failures for the payment process.
        PaymentStrategy retryStrategy = new PaymentRetryDecorator(rawStrategy);

        // Outer:  'Validation' logic
        // checks the database first. If validation fails, noneed for retrying.
        PaymentStrategy finalStrategy = new PaymentValidationDecorator(retryStrategy, orderRepository);

        return finalStrategy;

    }
}