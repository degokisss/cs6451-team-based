package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.base.notification.Notification;
import com.example.hotelreservationsystem.base.notification.NotificationServiceFactory;
import com.example.hotelreservationsystem.base.pricing.OrderPriceObserver;
import com.example.hotelreservationsystem.dto.BookingCreateRequest;
import com.example.hotelreservationsystem.dto.BookingResponse;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.NotificationType;
import com.example.hotelreservationsystem.enums.OrderStatus;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import com.example.hotelreservationsystem.repository.OrderRepository;
import com.example.hotelreservationsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        savedOrder.setOrderStatus(OrderStatus.CONFIRMED);
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
}
