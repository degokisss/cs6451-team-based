package com.example.hotelreservationsystem.base.payment;
import com.example.hotelreservationsystem.enums.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Factory for creating payment provider instances.
 * This class returns a payment provider based on the selected payment type.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFactory {

    private final CreditCardPaymentProvider creditCardPaymentProvider;
    private final PayPalPaymentProvider payPalPaymentProvider;

    /**
     * Retrieves the appropriate payment provider for a given payment type.
     * @param paymentType The type of payment (e.g., CREDIT_CARD, PAYPAL).
     * @return The corresponding PaymentProvider instance.
     * @throws IllegalArgumentException if the payment type is unsupported.
     */
    public PaymentProvider getProvider(PaymentType paymentType) {
        switch (paymentType) {
            case CREDIT_CARD:
                return creditCardPaymentProvider;
            case PAYPAL:
                return payPalPaymentProvider;
            case BANK_TRANSFER:
                // TODO: Bank Transfer payment provider is not implemented yet.
                log.warn("Bank Transfer not implemented yet");
                return null;
            default:
                throw new IllegalArgumentException("Unsupported payment type: " + paymentType);
        }
    }
}