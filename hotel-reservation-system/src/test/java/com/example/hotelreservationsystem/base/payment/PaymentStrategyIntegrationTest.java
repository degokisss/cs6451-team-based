package com.example.hotelreservationsystem.base.payment;

import com.example.hotelreservationsystem.base.payment.PaymentFactory;
import com.example.hotelreservationsystem.base.payment.PaymentStrategy;
import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.enums.OrderStatus;
import com.example.hotelreservationsystem.enums.PaymentType;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import com.example.hotelreservationsystem.repository.HotelRepository;
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
 * Integration tests for Payment Strategy Pattern & Decorators.
 * Tests the PaymentFactory assembly and the Validation Decorator logic.
 */
@SpringBootTest
@Transactional
class PaymentStrategyIntegrationTest {

    @Autowired
    private PaymentFactory paymentFactory;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private RoomRepository roomRepository;

    private Order pendingOrder;
    private Order confirmedOrder;

    @BeforeEach
    void setUp() {
        // 1. Setup Data for Validation Decorator
        Hotel hotel = hotelRepository.save(Hotel.builder().name("Test Hotel").address("Address").build());


        RoomType roomType = roomTypeRepository.save(RoomType.builder()
                .name("Single")
                .price(100f)
                .capacity(1)
                .description("Test Room Type")
                .build());

        Room room = roomRepository.save(Room.builder()
                .roomNumber("101")
                .hotel(hotel)
                .roomType(roomType)
                .roomStatus(RoomStatus.VACANT)
                .build());


        Customer customer = customerRepository.save(Customer.builder()
                .name("John Doe")
                .email("john@test.com")
                .passwordHash("mockHash")
                .phoneNumber("1234567890")
                .role(com.example.hotelreservationsystem.enums.Role.USER)
                .enabled(true)
                .membershipTier(com.example.hotelreservationsystem.enums.MembershipTier.BRONZE)
                .build());

        // 2. Create a PENDING order (Valid for payment)

        pendingOrder = orderRepository.save(Order.builder()
                .customer(customer)
                .room(room)
                .orderStatus(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(100))
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .numberOfNights(1L)
                .checkInCode("PEND1234")
                .build());

        // 3. Create a CONFIRMED order (Invalid for payment - Duplicate payment check)

        confirmedOrder = orderRepository.save(Order.builder()
                .customer(customer)
                .room(room)
                .orderStatus(OrderStatus.CONFIRMED)
                .totalPrice(BigDecimal.valueOf(100))
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .numberOfNights(1L)
                .checkInCode("CONF1234")
                .build());
    }

    @Test
    void shouldExecuteCreditCardStrategySuccessfully() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(pendingOrder.getId());
        request.setPaymentType(PaymentType.CREDIT_CARD);

        PaymentStrategy strategy = paymentFactory.getStrategy(PaymentType.CREDIT_CARD);
        PaymentResponse response = strategy.pay(request);

        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getTransactionId());
        assertTrue(response.getTransactionId().startsWith("creditcard_"));
    }

    @Test
    void shouldExecutePayPalStrategySuccessfully() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(pendingOrder.getId());
        request.setPaymentType(PaymentType.PAYPAL);

        PaymentStrategy strategy = paymentFactory.getStrategy(PaymentType.PAYPAL);
        PaymentResponse response = strategy.pay(request);

        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getTransactionId().startsWith("paypal_"));
    }

    @Test
    void shouldFailValidationForConfirmedOrder() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(confirmedOrder.getId());
        request.setPaymentType(PaymentType.CREDIT_CARD);

        PaymentStrategy strategy = paymentFactory.getStrategy(PaymentType.CREDIT_CARD);

        assertThrows(IllegalStateException.class, () -> {
            strategy.pay(request);
        }, "Should throw exception because order is not PENDING");
    }

    @Test
    void shouldFailValidationForNonExistentOrder() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(99999L);
        request.setPaymentType(PaymentType.CREDIT_CARD);

        PaymentStrategy strategy = paymentFactory.getStrategy(PaymentType.CREDIT_CARD);

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.pay(request);
        });
    }
}