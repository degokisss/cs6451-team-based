package com.example.hotelreservationsystem.base.payment;

import com.example.hotelreservationsystem.base.payment.decorator.PaymentRetryDecorator;
import com.example.hotelreservationsystem.base.payment.decorator.PaymentValidationDecorator;
import com.example.hotelreservationsystem.enums.PaymentType;
import com.example.hotelreservationsystem.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component

public class PaymentFactory {
    private final OrderRepository orderRepository;
    private final Map<PaymentType, PaymentStrategy> strategyMap;

    /*
    public PaymentStrategy getStrategy(PaymentType paymentType)

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
     */

        // 1. reWrite Constructor to inject all available strategies
    public PaymentFactory(OrderRepository orderRepository, List<PaymentStrategy> strategies) {
        this.orderRepository = orderRepository;

        this.strategyMap = new HashMap<>();
        for (PaymentStrategy strategy : strategies) {
            PaymentType type = strategy.getType();


            // Ensure no duplicate strategies for the same payment type since we are using List injection by for loop
            if (this.strategyMap.containsKey(type)) {

                PaymentStrategy existing = this.strategyMap.get(type);
                throw new IllegalStateException(String.format(
                        "Duplicate payment type '%s' found. " +
                                "Existing: %s, New: %s. " +
                                "Each payment type must have exactly one strategy.",
                        type,
                        existing.getClass().getSimpleName(),
                        strategy.getClass().getSimpleName()
                ));
            }

            this.strategyMap.put(type, strategy);

        }
    }

         // 2. Build decorated strategy chain based on requested type
       public PaymentStrategy getStrategy(PaymentType paymentType) {


           PaymentStrategy rawStrategy = strategyMap.get(paymentType);

           if (rawStrategy == null) {
               throw new IllegalArgumentException("Invalid payment type: " + paymentType);
           }


           // 3. Wrap with decorators: Retry -> Validation -> Raw Strategy

           PaymentStrategy retryStrategy = new PaymentRetryDecorator(rawStrategy);
           PaymentStrategy validationStrategy = new PaymentValidationDecorator(retryStrategy, orderRepository);

           return validationStrategy;
           //When requested, return the fully decorated strategy
       }
   }


