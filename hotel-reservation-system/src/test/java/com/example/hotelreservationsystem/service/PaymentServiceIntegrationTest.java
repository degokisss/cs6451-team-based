package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.enums.OrderStatus;
import com.example.hotelreservationsystem.enums.PaymentType;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import com.example.hotelreservationsystem.repository.OrderRepository;
import com.example.hotelreservationsystem.repository.RoomRepository;
import com.example.hotelreservationsystem.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Payment Service & Observer Pattern.
 * Verifies that successful payments trigger observers to update Order status.
 */
@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Given: Basic entity setup
        RoomType roomType = roomTypeRepository.save(RoomType.builder()
                .name("Suite")
                .price(200f)
                .capacity(2)
                .description("Test Suite")
                .build());

        Room room = roomRepository.save(Room.builder()
                .roomNumber("202")
                .roomType(roomType)
                .hotel(null) // Assuming hotel is nullable or handled elsewhere, otherwise create a Hotel
                .build());

        Customer customer = customerRepository.save(Customer.builder()
                .name("Jane Doe")
                .email("jane@test.com")
                .passwordHash("hashedPwd")
                .phoneNumber("987654321")
                .role(com.example.hotelreservationsystem.enums.Role.USER)
                .enabled(true)
                .membershipTier(com.example.hotelreservationsystem.enums.MembershipTier.SILVER)
                .build());

        // Create a PENDING Order with ALL mandatory fields
        testOrder = orderRepository.save(Order.builder()
                .customer(customer)
                .room(room)
                .orderStatus(OrderStatus.PENDING) // Initial status
                .totalPrice(BigDecimal.valueOf(200))
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .numberOfNights(2L)
                .checkInCode("PAYTEST1")
                .build());
    }

    @Test
    void shouldUpdateOrderStatusToConfirmedOnPaymentSuccess() {
        // Given: The order is currently PENDING
        assertEquals(OrderStatus.PENDING, testOrder.getOrderStatus());

        // When: Execute Payment via Service
        PaymentResponse response = paymentService.executePayment(testOrder.getId(), PaymentType.CREDIT_CARD);

        // Then 1: Payment itself is successful
        assertEquals("SUCCESS", response.getStatus());

        // Then 2: Observer side effects occurred
        // Fetch fresh from DB
        Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();

        assertEquals(OrderStatus.CONFIRMED, updatedOrder.getOrderStatus(),
                "Order status should be updated to CONFIRMED by the PaymentStatusUpdateObserver");
    }

    @Test
    void shouldNotUpdateStatusIfPaymentFailsValidation() {
        // Given: Manually set order to CANCELLED so validation fails
        testOrder.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(testOrder);

        // When: Try to pay
        try {
            paymentService.executePayment(testOrder.getId(), PaymentType.PAYPAL);
        } catch (Exception e) {
            // Expected exception due to validation
        }

        // Then: Order status remains CANCELLED (Observer should NOT have run)
        Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, updatedOrder.getOrderStatus());
    }
}