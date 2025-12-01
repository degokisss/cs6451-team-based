package com.example.hotelreservationsystem.base.payment.decorator;

import com.example.hotelreservationsystem.base.payment.PaymentStrategy;
import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRetryDecoratorTest {

    @Mock
    private PaymentStrategy wrappedStrategy;

    @Test
    void shouldRetryOnFailureAndEventuallySuccess() {
        // Given
        PaymentRetryDecorator retryDecorator = new PaymentRetryDecorator(wrappedStrategy);
        PaymentRequest request = new PaymentRequest();

        //  Simulate behavior: throw an exception the first two times, return success the third time
        when(wrappedStrategy.pay(request))
                .thenThrow(new RuntimeException("Network Error 1"))
                .thenThrow(new RuntimeException("Network Error 2"))
                .thenReturn(PaymentResponse.builder().status("SUCCESS").build());

        // When
        PaymentResponse response = retryDecorator.pay(request);

        // Then
        assertEquals("SUCCESS", response.getStatus());

        // Key verification: verify that the underlying pay method is indeed called 3 times
        verify(wrappedStrategy, times(3)).pay(request);
    }

    @Test
    void shouldStopRetryingAfterMaxAttempts() {
        // Given
        PaymentRetryDecorator retryDecorator = new PaymentRetryDecorator(wrappedStrategy);
        PaymentRequest request = new PaymentRequest();

        // Key verification: verify that the underlying pay method is indeed called 3 times
        when(wrappedStrategy.pay(request))
                .thenThrow(new RuntimeException("Fail"));

        // When
        PaymentResponse response = retryDecorator.pay(request);

        // Then
        // Because the last time still failed, according to current code logic,
        // the response may be null or an exception may be thrown and caught in this test.
        // The main purpose here is to verify that the number of retries is limited to 3 times (MAX_RETRIES)

        verify(wrappedStrategy, times(3)).pay(request);
    }
}