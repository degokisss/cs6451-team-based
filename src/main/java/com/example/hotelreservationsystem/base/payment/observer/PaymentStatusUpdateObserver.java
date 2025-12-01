package com.example.hotelreservationsystem.base.payment.observer;

import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.repository.OrderRepository;
import com.example.hotelreservationsystem.service.roomstate.ReservationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

/**
 * [Observer Pattern]
 * Concrete Observer #2 ：update order status after payment success
 * Job: After successful payment, change order status to CONFIRMED
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentStatusUpdateObserver implements PaymentObserver {

    private final OrderRepository orderRepository;


    private final TransactionTemplate transactionTemplate;

    @Override

    public void onPaymentSuccess(Long orderId, PaymentResponse response) {
        log.info("Payment success for Order ID: {}. Updating status to CONFIRMED...", orderId);

        int maxRetries = 3;
        int attempt = 0;
        long waitTime = 2000L; // 2s


        while (attempt < maxRetries) {
            attempt++;
            try {

                transactionTemplate.executeWithoutResult(status -> {
                    doUpdateOrderStatus(orderId);
                });

                log.info("Order {} status updated to CONFIRMED successfully on attempt {}.", orderId, attempt);
                return;
            } catch (Exception e) {
                log.warn("Attempt {}/{} failed to update order {}. Error: {}",
                        attempt, maxRetries, orderId, e.getMessage());

               //if not the last attempt, wait before retrying
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    // after all retries, still fail - log critical error for manual intervention
                    handleFinalFailure(orderId, response, e);
                }
            }
        }
    }
    // extracted method to update order status
    private void doUpdateOrderStatus(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new IllegalStateException("Order not found: " + orderId);
        }

        Order order = orderOpt.get();
        //state pattern to confirm reservation
        ReservationContext context = new ReservationContext(order);
        context.confirm();

        orderRepository.save(order);
    }

    // handle final failure after all retries
    private void handleFinalFailure(Long orderId, PaymentResponse response, Exception e) {
        log.error("CRITICAL: Warning ! Payment Successful but failed to update DB for Order {} after all retries. " +
                        "Transaction ID: {}. Final Error: {}",
                orderId, response.getTransactionId(), e.getMessage());

        // maybe send alert to admin or create a ticket for manual intervention
    }
}