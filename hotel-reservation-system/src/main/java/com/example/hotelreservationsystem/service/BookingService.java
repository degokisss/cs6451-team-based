package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.base.notification.Notification;
import com.example.hotelreservationsystem.base.notification.NotificationServiceFactory;
import com.example.hotelreservationsystem.base.pricing.OrderPriceObserver;
import com.example.hotelreservationsystem.dto.BookingCreateRequest;
import com.example.hotelreservationsystem.dto.BookingResponse;
import com.example.hotelreservationsystem.dto.CancellationResponse;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.NotificationType;
import com.example.hotelreservationsystem.enums.OrderStatus;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import com.example.hotelreservationsystem.repository.OrderRepository;
import com.example.hotelreservationsystem.repository.RoomRepository;
import com.example.hotelreservationsystem.service.state.ReservationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;

/**
 * Service for managing booking orders
 * Handles booking creation from locks with State Pattern implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final OrderRepository orderRepository;
    private final BookingLockService bookingLockService;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final NotificationServiceFactory notificationServiceFactory;
    private final RoomService roomService;

    /**
     * Get a booking order by ID for the authenticated customer.
     *
     * @param orderId The order ID to retrieve
     * @param customerId The authenticated customer ID
     * @return BookingResponse with order details
     * @throws IllegalArgumentException if order not found
     * @throws SecurityException if order does not belong to the customer
     */
    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new SecurityException("You are not authorized to view this order");
        }

        return toBookingResponse(order);
    }

    /**
     * Get a booking order by room for the authenticated customer.
     *
     * @param roomId The room ID to retrieve the current order for
     * @param customerId The authenticated customer ID
     * @return BookingResponse with order details
     * @throws IllegalArgumentException if no order exists for the room
     * @throws SecurityException if order does not belong to the customer
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingByRoom(Long roomId, Long customerId) {
        Order order = orderRepository.findByRoomId(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found for room: " + roomId));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new SecurityException("You are not authorized to view this order");
        }

        return toBookingResponse(order);
    }

    private BookingResponse toBookingResponse(Order order) {
        return BookingResponse.builder()
            .orderId(order.getId())
            .customerId(order.getCustomer().getId())
            .roomId(order.getRoom().getId())
            .orderStatus(order.getOrderStatus())
            .totalPrice(order.getTotalPrice())
            .checkInDate(order.getCheckInDate())
            .checkOutDate(order.getCheckOutDate())
            .createdAt(order.getCreatedAt())
            .checkInCode(order.getCheckInCode())
            .build();
    }

    /**
     * Create a booking order from a lock
     *
     * @param request Booking creation request with lock ID and booking details
     * @return BookingResponse with order details
     * @throws IllegalArgumentException if lock is invalid, expired, or belongs to another customer
     * @throws IllegalStateException if room or customer not found
     */
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        log.info("Creating booking from lock: {} for room: {} and customer: {}",
            request.getLockId(), request.getRoomId(), request.getCustomerId());

        // Step 1: Validate lock exists for the room
        Map<String, Object> lockInfo = bookingLockService.getLockInfo(request.getRoomId());

        if (lockInfo == null) {
            log.warn("Lock not found or expired for room: {}", request.getRoomId());
            throw new IllegalArgumentException("Invalid or expired lock");
        }

        // Step 2: Validate lock details match request
        String lockId = (String) lockInfo.get("lockId");
        Long lockCustomerId = ((Number) lockInfo.get("customerId")).longValue();
        Long roomId = ((Number) lockInfo.get("roomId")).longValue();

        if (!lockId.equals(request.getLockId())) {
            log.warn("Lock ID mismatch: expected {}, got {}", lockId, request.getLockId());
            throw new IllegalArgumentException("Invalid lock ID");
        }

        if (!lockCustomerId.equals(request.getCustomerId())) {
            log.warn("Customer {} attempted to use lock owned by customer {}",
                request.getCustomerId(), lockCustomerId);
            throw new SecurityException("Lock belongs to another customer");
        }

        // Step 3: Get room and customer entities
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalStateException("Room not found: " + roomId));

        Customer customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new IllegalStateException("Customer not found: " + request.getCustomerId()));

        // Step 4: Calculate total price
        long numberOfNights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (numberOfNights <= 0) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        BigDecimal roomPrice = BigDecimal.valueOf(room.getRoomType().getPrice());
        BigDecimal totalPrice = roomPrice.multiply(BigDecimal.valueOf(numberOfNights));

        log.info("Calculated price: {} × {} nights = {}", roomPrice, numberOfNights, totalPrice);

        // Step 5: Generate unique check-in code
        String checkInCode = generateCheckInCode();
        log.info("Generated check-in code: {}", checkInCode);

        // Step 6: Create Order entity with PENDING status
        Order order = Order.builder()
            .customer(customer)
            .room(room)
            .checkInDate(request.getCheckInDate())
            .checkOutDate(request.getCheckOutDate())
            .numberOfNights(numberOfNights)
            .totalPrice(totalPrice)
            .orderStatus(OrderStatus.PENDING)
            .checkInCode(checkInCode)
            .build();

        // Step 7: Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Created order {} with PENDING status and check-in code {}", savedOrder.getId(), checkInCode);

        // Step 8: Transition to CONFIRMED status (auto-confirm for now)
        new ReservationContext(savedOrder).confirm();
        savedOrder = orderRepository.save(savedOrder);
        log.info("Order {} transitioned to CONFIRMED", savedOrder.getId());

        // Step 8.5: Register order as observer for room price changes (Observer Pattern)
        Long roomTypeId = room.getRoomType().getId();
        OrderPriceObserver priceObserver = new OrderPriceObserver(savedOrder, orderRepository);
        roomService.addObserver(roomTypeId, priceObserver);
        log.info("Registered Order {} as price observer for RoomType ID: {}", savedOrder.getId(), roomTypeId);

        // Step 9: Release lock from Redis
        boolean lockReleased = bookingLockService.releaseLock(request.getLockId(), request.getCustomerId());
        if (lockReleased) {
            log.info("Released lock {} after order creation", request.getLockId());
        } else {
            log.warn("Failed to release lock {} after order creation", request.getLockId());
        }

        // Step 10: Send notification to customer with check-in code
        sendBookingConfirmationNotifications(customer, savedOrder, room);

        // Step 11: Return response
        return BookingResponse.builder()
            .orderId(savedOrder.getId())
            .customerId(savedOrder.getCustomer().getId())
            .roomId(savedOrder.getRoom().getId())
            .orderStatus(savedOrder.getOrderStatus())
            .totalPrice(savedOrder.getTotalPrice())
            .checkInDate(savedOrder.getCheckInDate())
            .checkOutDate(savedOrder.getCheckOutDate())
            .createdAt(savedOrder.getCreatedAt())
            .checkInCode(savedOrder.getCheckInCode())
            .build();
    }

    /**
     * Generate a random 8-character alphanumeric check-in code
     * Format: XXXXXXXX (uppercase letters and numbers)
     *
     * @return Random check-in code
     */
    private String generateCheckInCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }

        return code.toString();
    }

    /**
     * Send booking confirmation notifications via Email and SMS
     *
     * @param customer Customer to notify
     * @param order Confirmed order with check-in code
     * @param room Booked room details
     */
    private void sendBookingConfirmationNotifications(Customer customer, Order order, Room room) {
        try {
            // Send Email notification
            Notification<SimpleMailMessage> emailService = notificationServiceFactory.createNotificationService(NotificationType.EMAIL);
            SimpleMailMessage emailMessage = createBookingConfirmationEmail(customer, order, room);
            boolean emailSent = emailService.sendNotification(emailMessage);

            if (emailSent) {
                log.info("Booking confirmation email sent to {} for order {}", customer.getEmail(), order.getId());
            } else {
                log.warn("Failed to send booking confirmation email to {} for order {}", customer.getEmail(), order.getId());
            }

            // Send SMS notification
            Notification<String> smsService = notificationServiceFactory.createNotificationService(NotificationType.SMS);
            String smsMessage = createBookingConfirmationSMS(order, room);
            boolean smsSent = smsService.sendNotification(smsMessage);

            if (smsSent) {
                log.info("Booking confirmation SMS sent to {} for order {}", customer.getPhoneNumber(), order.getId());
            } else {
                log.warn("Failed to send booking confirmation SMS to {} for order {}", customer.getPhoneNumber(), order.getId());
            }

        } catch (Exception e) {
            log.error("Error sending booking confirmation notifications for order {}: {}", order.getId(), e.getMessage(), e);
            // Don't throw exception - notification failure should not block booking creation
        }
    }

    /**
     * Create email message for booking confirmation
     */
    private SimpleMailMessage createBookingConfirmationEmail(Customer customer, Order order, Room room) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(customer.getEmail());
        message.setSubject("Booking Confirmation - Order #" + order.getId());

        String emailBody = String.format("""
            Dear %s,

            Your booking has been confirmed!

            Booking Details:
            - Order ID: %d
            - Room: %s
            - Check-in Date: %s
            - Check-out Date: %s
            - Total Price: $%.2f

            CHECK-IN CODE: %s

            Please save this check-in code. You will need it for automatic check-in at the hotel.
            You can also use this code to generate a QR code for contactless check-in.

            Thank you for choosing our hotel!

            Best regards,
            Hotel Reservation System
            """,
            customer.getName(),
            order.getId(),
            room.getRoomNumber() + " - " + room.getRoomType().getName(),
            order.getCheckInDate(),
            order.getCheckOutDate(),
            order.getTotalPrice(),
            order.getCheckInCode()
        );

        message.setText(emailBody);
        return message;
    }

    /**
     * Create SMS message for booking confirmation
     */
    private String createBookingConfirmationSMS(Order order, Room room) {
        return String.format(
            "Booking Confirmed! Order #%d | Room: %s | Check-in: %s | CHECK-IN CODE: %s | Save this code for check-in.",
            order.getId(),
            room.getRoomNumber(),
            order.getCheckInDate(),
            order.getCheckInCode()
        );
    }

    /**
     * Cancel a booking order
     *
     * @param orderId Order ID to cancel
     * @param customerId Customer ID requesting cancellation
     * @param cancellationReason Optional reason for cancellation
     * @return CancellationResponse with cancellation details
     * @throws IllegalArgumentException if order not found
     * @throws SecurityException if customer doesn't own the order
     * @throws IllegalStateException if order cannot be cancelled (already CANCELLED or COMPLETED)
     */
    @Transactional
    public CancellationResponse cancelBooking(Long orderId, Long customerId, String cancellationReason) {
        log.info("Cancelling booking order: {} for customer: {}", orderId, customerId);

        // Step 1: Validate order exists
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.warn("Order not found: {}", orderId);
                return new IllegalArgumentException("Order not found with ID: " + orderId);
            });

        // Step 2: Validate order belongs to customer
        if (!order.getCustomer().getId().equals(customerId)) {
            log.warn("Customer {} attempted to cancel order {} owned by customer {}",
                customerId, orderId, order.getCustomer().getId());
            throw new SecurityException("You are not authorized to cancel this order");
        }

        // Step 3: Validate order status is cancellable (PENDING or CONFIRMED only)
        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus == OrderStatus.CANCELLED) {
            log.warn("Order {} is already cancelled", orderId);
            throw new IllegalStateException("Order is already cancelled");
        }
        if (currentStatus == OrderStatus.COMPLETED) {
            log.warn("Order {} cannot be cancelled - already completed", orderId);
            throw new IllegalStateException("Cannot cancel completed order");
        }

        // Step 4: Update order status to CANCELLED
        OrderStatus previousStatus = order.getOrderStatus();
        new ReservationContext(order).cancel();
        log.info("Order {} status changed: {} → CANCELLED", orderId, previousStatus);

        // Step 5: Set cancellation timestamp
        LocalDateTime cancelledAt = LocalDateTime.now();
        order.setCancelledAt(cancelledAt);

        // Step 6: Store cancellation reason if provided
        if (cancellationReason != null && !cancellationReason.trim().isEmpty()) {
            order.setCancellationReason(cancellationReason);
            log.info("Cancellation reason recorded for order {}: {}", orderId, cancellationReason);
        }

        // Step 7: Save updated order
        Order cancelledOrder = orderRepository.save(order);
        log.info("Order {} cancelled successfully at {}", orderId, cancelledAt);

        // Step 8: Unregister order from price observers (Observer Pattern cleanup)
        try {
            Long roomTypeId = order.getRoom().getRoomType().getId();
            roomService.removeObserver(roomTypeId, cancelledOrder);
            log.info("Unregistered Order {} from price observer for RoomType ID: {}", orderId, roomTypeId);
        } catch (Exception e) {
            log.warn("Failed to unregister order {} from price observer: {}", orderId, e.getMessage());
            // Don't fail cancellation if observer cleanup fails
        }

        // Step 9: Send cancellation notification to customer
        sendCancellationNotifications(order.getCustomer(), cancelledOrder);

        // Step 10: Return cancellation response
        return CancellationResponse.builder()
            .orderId(cancelledOrder.getId())
            .previousStatus(previousStatus)
            .cancelledAt(cancelledAt)
            .cancellationReason(cancellationReason)
            .message("Booking cancelled successfully")
            .build();
    }

    /**
     * Send cancellation notifications via Email and SMS
     *
     * @param customer Customer to notify
     * @param order Cancelled order details
     */
    private void sendCancellationNotifications(Customer customer, Order order) {
        try {
            // Send Email notification
            Notification<SimpleMailMessage> emailService = notificationServiceFactory.createNotificationService(NotificationType.EMAIL);
            SimpleMailMessage emailMessage = createCancellationEmail(customer, order);
            boolean emailSent = emailService.sendNotification(emailMessage);

            if (emailSent) {
                log.info("Cancellation email sent to {} for order {}", customer.getEmail(), order.getId());
            } else {
                log.warn("Failed to send cancellation email to {} for order {}", customer.getEmail(), order.getId());
            }

            // Send SMS notification
            Notification<String> smsService = notificationServiceFactory.createNotificationService(NotificationType.SMS);
            String smsMessage = createCancellationSMS(order);
            boolean smsSent = smsService.sendNotification(smsMessage);

            if (smsSent) {
                log.info("Cancellation SMS sent to {} for order {}", customer.getPhoneNumber(), order.getId());
            } else {
                log.warn("Failed to send cancellation SMS to {} for order {}", customer.getPhoneNumber(), order.getId());
            }

        } catch (Exception e) {
            log.error("Error sending cancellation notifications for order {}: {}", order.getId(), e.getMessage(), e);
            // Don't throw exception - notification failure should not block cancellation
        }
    }

    /**
     * Create email message for cancellation confirmation
     */
    private SimpleMailMessage createCancellationEmail(Customer customer, Order order) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(customer.getEmail());
        message.setSubject("Booking Cancelled - Order #" + order.getId());

        String emailBody = String.format("""
            Dear %s,

            Your booking has been cancelled.

            Cancellation Details:
            - Order ID: %d
            - Room: %s
            - Original Check-in Date: %s
            - Original Check-out Date: %s
            - Total Price: $%.2f
            - Cancelled At: %s
            %s

            According to our cancellation policy, refunds will be processed within 5-7 business days.

            If you did not request this cancellation, please contact us immediately.

            Thank you for your understanding.

            Best regards,
            Hotel Reservation System
            """,
            customer.getName(),
            order.getId(),
            order.getRoom().getRoomNumber() + " - " + order.getRoom().getRoomType().getName(),
            order.getCheckInDate(),
            order.getCheckOutDate(),
            order.getTotalPrice(),
            order.getCancelledAt(),
            order.getCancellationReason() != null ? "- Reason: " + order.getCancellationReason() : ""
        );

        message.setText(emailBody);
        return message;
    }

    /**
     * Create SMS message for cancellation confirmation
     */
    private String createCancellationSMS(Order order) {
        return String.format(
            "Booking Cancelled - Order #%d | Room: %s | Cancelled at: %s | Refund will be processed in 5-7 business days.",
            order.getId(),
            order.getRoom().getRoomNumber(),
            order.getCancelledAt()
        );
    }
}
