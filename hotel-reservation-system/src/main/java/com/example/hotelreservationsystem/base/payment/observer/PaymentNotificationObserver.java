package com.example.hotelreservationsystem.base.payment.observer;

import com.example.hotelreservationsystem.base.notification.Notification;
import com.example.hotelreservationsystem.base.notification.NotificationServiceFactory;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.enums.NotificationType;
import com.example.hotelreservationsystem.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * [Observer Pattern]
 * Concrete Observer #2： send payment receipt email after payment success
 * Job: After successful payment, send receipt email to customer
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentNotificationObserver implements PaymentObserver {

    // Inject NotificationServiceFactory to create email service
    private final NotificationServiceFactory notificationServiceFactory;

    // Inject OrderRepository to query customer email and amount
    private final OrderRepository orderRepository;

    @Override
    public void onPaymentSuccess(Long orderId, PaymentResponse response) {
        log.info("Preparing <<PAYMENT RECEIPT>> for Order {}", orderId);

        // Step 1: Query order details (to get customer email and amount)
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            log.error("Sorry, Order {} not found. Cannot send receipt.Please check again.", orderId);
            return;
        }
        Order order = orderOpt.get();
        String customerEmail = order.getCustomer().getEmail();

        // Step 2: Build email content (Receipt)
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(customerEmail);
        message.setSubject("Payment Receipt - Order #" + orderId);

        //  receipt format
        String receiptBody = String.format("""
            Dear %s,
            
            Payment Successful! Here is your receipt.
            
            ================================
            PAYMENT RECEIPT
            ================================
            Order ID       : %d
            Transaction ID : %s
            Total Amount   : $%.2f
            Payment Status : %s
            Date           : %s
            ================================
            
            Thank you for choosing our hotel, hope you enjoy!
            """,
                order.getCustomer().getName(),
                orderId,
                response.getTransactionId(),
                order.getTotalPrice(),
                response.getStatus(),
                java.time.LocalDate.now()
        );
        message.setText(receiptBody);

        // Step 3: Get service from factory and send
        try {
            // Use factory pattern to create/get EMAIL type notification service
            Notification<SimpleMailMessage> emailService = notificationServiceFactory.createNotificationService(NotificationType.EMAIL);

            // Send notification
            boolean sent = emailService.sendNotification(message);

            if (sent) {
                log.info("Receipt sent successfully to {}", customerEmail);
            } else {
                log.warn("Failed to send receipt.");
            }

        } catch (Exception e) {
            log.error("Error when sending receipt: {}", e.getMessage());
        }
    }
}