package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.base.notification.Notification;
import com.example.hotelreservationsystem.base.notification.NotificationServiceFactory;
import com.example.hotelreservationsystem.dto.BookingCreateRequest;
import com.example.hotelreservationsystem.dto.BookingResponse;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.enums.NotificationType;
import com.example.hotelreservationsystem.enums.OrderStatus;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import com.example.hotelreservationsystem.repository.OrderRepository;
import com.example.hotelreservationsystem.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BookingLockService bookingLockService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private NotificationServiceFactory notificationServiceFactory;

    @Mock
    private Notification<SimpleMailMessage> emailNotification;

    @Mock
    private Notification<String> smsNotification;

    @Mock
    private RoomService roomService;

    private BookingService bookingService;

    private static final String TEST_LOCK_ID = "test-lock-123";
    private static final Long TEST_ROOM_ID = 1L;
    private static final Long TEST_CUSTOMER_ID = 100L;
    private static final LocalDate CHECK_IN_DATE = LocalDate.now().plusDays(7);
    private static final LocalDate CHECK_OUT_DATE = LocalDate.now().plusDays(10);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Configure notification service mocks (lenient because not all tests reach notification code)
        try {
            lenient().when(notificationServiceFactory.createNotificationService(NotificationType.EMAIL))
                .thenReturn((Notification) emailNotification);
            lenient().when(notificationServiceFactory.createNotificationService(NotificationType.SMS))
                .thenReturn((Notification) smsNotification);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        lenient().when(emailNotification.sendNotification(any(SimpleMailMessage.class))).thenReturn(true);
        lenient().when(smsNotification.sendNotification(anyString())).thenReturn(true);

        bookingService = new BookingService(
            orderRepository,
            bookingLockService,
            roomRepository,
            customerRepository,
            notificationServiceFactory,
            roomService
        );
    }

    @Test
    void shouldCreateBookingSuccessfully() {
        // Given
        BookingCreateRequest request = createValidRequest();
        Map<String, Object> lockInfo = createValidLockInfo();
        Room room = createTestRoom();
        Customer customer = createTestCustomer();
        Order savedOrder = createTestOrder();

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(room));
        when(customerRepository.findById(TEST_CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(bookingLockService.releaseLock(TEST_LOCK_ID, TEST_CUSTOMER_ID)).thenReturn(true);

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getOrderId());
        assertEquals(TEST_CUSTOMER_ID, response.getCustomerId());
        assertEquals(TEST_ROOM_ID, response.getRoomId());
        assertEquals(OrderStatus.CONFIRMED, response.getOrderStatus());
        assertEquals(new BigDecimal("300.00"), response.getTotalPrice()); // 100 * 3 nights
        assertNotNull(response.getCheckInCode());
        assertEquals(8, response.getCheckInCode().length());

        verify(bookingLockService).getLockInfo(TEST_ROOM_ID);
        verify(orderRepository, times(2)).save(any(Order.class)); // PENDING then CONFIRMED
        verify(bookingLockService).releaseLock(TEST_LOCK_ID, TEST_CUSTOMER_ID);
    }

    @Test
    void shouldCalculatePriceCorrectly() {
        // Given - 5 nights at 100 per night = 500
        BookingCreateRequest request = BookingCreateRequest.builder()
            .lockId(TEST_LOCK_ID)
            .roomId(TEST_ROOM_ID)
            .customerId(TEST_CUSTOMER_ID)
            .guestName("Test Guest")
            .guestEmail("test@example.com")
            .guestPhone("1234567890")
            .checkInDate(LocalDate.now().plusDays(7))
            .checkOutDate(LocalDate.now().plusDays(12)) // 5 nights
            .build();

        Map<String, Object> lockInfo = createValidLockInfo();
        Room room = createTestRoom();
        Customer customer = createTestCustomer();

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(room));
        when(customerRepository.findById(TEST_CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(bookingLockService.releaseLock(TEST_LOCK_ID, TEST_CUSTOMER_ID)).thenReturn(true);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setCreatedAt(LocalDateTime.now());
            return order;
        });

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        Order capturedOrder = orderCaptor.getAllValues().getFirst(); // First save (PENDING)
        assertEquals(0, new BigDecimal("500.00").compareTo(capturedOrder.getTotalPrice()));
        assertEquals(0, new BigDecimal("500.00").compareTo(response.getTotalPrice()));
    }

    @Test
    void shouldThrowExceptionWhenLockNotFound() {
        // Given
        BookingCreateRequest request = createValidRequest();
        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(null);

        // When / Then
        var exception = assertThrows(IllegalArgumentException.class, () ->
            bookingService.createBooking(request)
        );

        assertEquals("Invalid or expired lock", exception.getMessage());
        verify(bookingLockService).getLockInfo(TEST_ROOM_ID);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldThrowExceptionWhenLockBelongsToAnotherCustomer() {
        // Given
        BookingCreateRequest request = createValidRequest();
        Map<String, Object> lockInfo = createValidLockInfo();
        lockInfo.put("customerId", 999L); // Different customer

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);

        // When / Then
        var exception = assertThrows(SecurityException.class, () ->
            bookingService.createBooking(request)
        );

        assertEquals("Lock belongs to another customer", exception.getMessage());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldThrowExceptionWhenLockIdMismatch() {
        // Given
        BookingCreateRequest request = createValidRequest();
        Map<String, Object> lockInfo = createValidLockInfo();
        lockInfo.put("lockId", "different-lock-id");

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);

        // When / Then
        var exception = assertThrows(IllegalArgumentException.class, () ->
            bookingService.createBooking(request)
        );

        assertEquals("Invalid lock ID", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRoomNotFound() {
        // Given
        BookingCreateRequest request = createValidRequest();
        Map<String, Object> lockInfo = createValidLockInfo();

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.empty());

        // When / Then
        var exception = assertThrows(IllegalStateException.class, () ->
            bookingService.createBooking(request)
        );

        assertTrue(exception.getMessage().contains("Room not found"));
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        BookingCreateRequest request = createValidRequest();
        Map<String, Object> lockInfo = createValidLockInfo();
        Room room = createTestRoom();

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(room));
        when(customerRepository.findById(TEST_CUSTOMER_ID)).thenReturn(Optional.empty());

        // When / Then
        var exception = assertThrows(IllegalStateException.class, () ->
            bookingService.createBooking(request)
        );

        assertTrue(exception.getMessage().contains("Customer not found"));
    }

    @Test
    void shouldThrowExceptionWhenCheckOutBeforeCheckIn() {
        // Given
        BookingCreateRequest request = BookingCreateRequest.builder()
            .lockId(TEST_LOCK_ID)
            .roomId(TEST_ROOM_ID)
            .customerId(TEST_CUSTOMER_ID)
            .guestName("Test Guest")
            .guestEmail("test@example.com")
            .guestPhone("1234567890")
            .checkInDate(LocalDate.now().plusDays(10))
            .checkOutDate(LocalDate.now().plusDays(7)) // Before check-in!
            .build();

        Map<String, Object> lockInfo = createValidLockInfo();
        Room room = createTestRoom();
        Customer customer = createTestCustomer();

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(room));
        when(customerRepository.findById(TEST_CUSTOMER_ID)).thenReturn(Optional.of(customer));

        // When / Then
        var exception = assertThrows(IllegalArgumentException.class, () ->
            bookingService.createBooking(request)
        );

        assertTrue(exception.getMessage().contains("Check-out date must be after check-in date"));
    }

    @Test
    void shouldTransitionFromPendingToConfirmed() {
        // Given
        BookingCreateRequest request = createValidRequest();
        Map<String, Object> lockInfo = createValidLockInfo();
        Room room = createTestRoom();
        Customer customer = createTestCustomer();

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(room));
        when(customerRepository.findById(TEST_CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(bookingLockService.releaseLock(TEST_LOCK_ID, TEST_CUSTOMER_ID)).thenReturn(true);

        // Capture the status at the time of each save call
        var capturedStatuses = new java.util.ArrayList<OrderStatus>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            capturedStatuses.add(order.getOrderStatus()); // Capture status at call time
            order.setId(1L);
            order.setCreatedAt(LocalDateTime.now());
            return order;
        });

        // When
        bookingService.createBooking(request);

        // Then
        verify(orderRepository, times(2)).save(any(Order.class));
        assertEquals(2, capturedStatuses.size());
        // First save should be PENDING, second save should be CONFIRMED
        assertEquals(OrderStatus.PENDING, capturedStatuses.get(0));
        assertEquals(OrderStatus.CONFIRMED, capturedStatuses.get(1));
    }

    @Test
    void shouldReleaseLockAfterOrderCreation() {
        // Given
        BookingCreateRequest request = createValidRequest();
        Map<String, Object> lockInfo = createValidLockInfo();
        Room room = createTestRoom();
        Customer customer = createTestCustomer();
        Order savedOrder = createTestOrder();

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(room));
        when(customerRepository.findById(TEST_CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(bookingLockService.releaseLock(TEST_LOCK_ID, TEST_CUSTOMER_ID)).thenReturn(true);

        // When
        bookingService.createBooking(request);

        // Then
        verify(bookingLockService).releaseLock(TEST_LOCK_ID, TEST_CUSTOMER_ID);
    }

    @Test
    void shouldRegisterOrderAsObserverForPriceChanges() {
        // Given
        BookingCreateRequest request = createValidRequest();
        Map<String, Object> lockInfo = createValidLockInfo();
        Room room = createTestRoom();
        Customer customer = createTestCustomer();
        Order savedOrder = createTestOrder();

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(room));
        when(customerRepository.findById(TEST_CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(bookingLockService.releaseLock(TEST_LOCK_ID, TEST_CUSTOMER_ID)).thenReturn(true);

        // When
        bookingService.createBooking(request);

        // Then - Verify observer was registered for the room type
        Long roomTypeId = room.getRoomType().getId();
        verify(roomService).addObserver(eq(roomTypeId), any());
    }

    @Test
    void shouldUpdateOrderPriceWhenRoomPriceChanges() {
        // Given - Create an order with 3 nights at $100/night = $300 total
        Order order = Order.builder()
            .checkInDate(CHECK_IN_DATE)
            .checkOutDate(CHECK_OUT_DATE)
            .numberOfNights(3L)
            .totalPrice(new BigDecimal("300.00"))
            .orderStatus(OrderStatus.CONFIRMED)
            .build();
        order.setId(1L);

        // When - Price changes to $150/night
        float newPrice = 150.0f;
        order.update(newPrice);

        // Then - Total should be $150 × 3 nights = $450
        assertEquals(0, new BigDecimal("450.00").compareTo(order.getTotalPrice()));
    }

    // Helper methods
    private BookingCreateRequest createValidRequest() {
        return BookingCreateRequest.builder()
            .lockId(TEST_LOCK_ID)
            .roomId(TEST_ROOM_ID)
            .customerId(TEST_CUSTOMER_ID)
            .guestName("Test Guest")
            .guestEmail("test@example.com")
            .guestPhone("1234567890")
            .checkInDate(CHECK_IN_DATE)
            .checkOutDate(CHECK_OUT_DATE)
            .build();
    }

    private Map<String, Object> createValidLockInfo() {
        Map<String, Object> lockInfo = new HashMap<>();
        lockInfo.put("lockId", TEST_LOCK_ID);
        lockInfo.put("customerId", TEST_CUSTOMER_ID);
        lockInfo.put("roomId", TEST_ROOM_ID);
        lockInfo.put("timestamp", LocalDateTime.now().toString());
        return lockInfo;
    }

    private Room createTestRoom() {
        RoomType roomType = RoomType.builder()
            .name("Standard")
            .price(100.0f)
            .build();

        Room room = Room.builder()
            .roomNumber("101")
            .roomType(roomType)
            .build();
        room.setId(TEST_ROOM_ID);
        return room;
    }

    private Customer createTestCustomer() {
        Customer customer = Customer.builder()
            .name("Test Customer")
            .email("customer@example.com")
            .build();
        customer.setId(TEST_CUSTOMER_ID);
        return customer;
    }

    private Order createTestOrder() {
        Order order = Order.builder()
            .customer(createTestCustomer())
            .room(createTestRoom())
            .checkInDate(CHECK_IN_DATE)
            .checkOutDate(CHECK_OUT_DATE)
            .numberOfNights(3L)
            .totalPrice(new BigDecimal("300.00"))
            .orderStatus(OrderStatus.CONFIRMED)
            .checkInCode("ABCD1234")
            .build();
        order.setId(1L);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}
