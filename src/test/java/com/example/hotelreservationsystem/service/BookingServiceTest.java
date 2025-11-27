package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.base.notification.Notification;
import com.example.hotelreservationsystem.base.notification.NotificationServiceFactory;
import com.example.hotelreservationsystem.dto.BookingCreateRequest;
import com.example.hotelreservationsystem.dto.BookingResponse;
import com.example.hotelreservationsystem.dto.CancellationResponse;
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
        assertEquals(OrderStatus.PENDING, response.getOrderStatus()); // Order starts as PENDING, transitions to CONFIRMED after payment
        assertEquals(new BigDecimal("300.00"), response.getTotalPrice()); // 100 * 3 nights
        assertNotNull(response.getCheckInCode());
        assertEquals(8, response.getCheckInCode().length());

        verify(bookingLockService).getLockInfo(TEST_ROOM_ID);
        verify(orderRepository, times(1)).save(any(Order.class)); // Only PENDING (CONFIRMED happens via PaymentStatusUpdateObserver)
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
    void shouldCreateOrderWithPendingStatus() {
        // Given
        // Note: Transition to CONFIRMED now happens via PaymentStatusUpdateObserver after payment
        BookingCreateRequest request = createValidRequest();
        Map<String, Object> lockInfo = createValidLockInfo();
        Room room = createTestRoom();
        Customer customer = createTestCustomer();

        when(bookingLockService.getLockInfo(TEST_ROOM_ID)).thenReturn(lockInfo);
        when(roomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(room));
        when(customerRepository.findById(TEST_CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(bookingLockService.releaseLock(TEST_LOCK_ID, TEST_CUSTOMER_ID)).thenReturn(true);

        // Capture the status at the time of save call
        var capturedStatuses = new java.util.ArrayList<OrderStatus>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            capturedStatuses.add(order.getOrderStatus()); // Capture status at call time
            order.setId(1L);
            order.setCreatedAt(LocalDateTime.now());
            return order;
        });

        // When
        BookingResponse response = bookingService.createBooking(request);

        // Then
        verify(orderRepository, times(1)).save(any(Order.class));
        assertEquals(1, capturedStatuses.size());
        // Order should be created in PENDING status
        assertEquals(OrderStatus.PENDING, capturedStatuses.get(0));
        assertEquals(OrderStatus.PENDING, response.getOrderStatus());
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
            .orderStatus(OrderStatus.PENDING) // Orders start as PENDING, transition to CONFIRMED via PaymentStatusUpdateObserver
            .checkInCode("ABCD1234")
            .build();
        order.setId(1L);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    // ============================================
    // Cancellation Tests
    // ============================================

    @Test
    void shouldCancelPendingOrder() {
        // Given
        Order order = createTestOrder();
        order.setOrderStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CancellationResponse response = bookingService.cancelBooking(1L, TEST_CUSTOMER_ID, "Change of plans");

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getOrderId());
        assertEquals(OrderStatus.PENDING, response.getPreviousStatus());
        assertNotNull(response.getCancelledAt());
        assertEquals("Change of plans", response.getCancellationReason());
        assertEquals("Booking cancelled successfully", response.getMessage());

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertNotNull(order.getCancelledAt());
        assertEquals("Change of plans", order.getCancellationReason());

        verify(orderRepository).save(order);
        verify(roomService).removeObserver(any(), any());
    }

    @Test
    void shouldCancelConfirmedOrder() {
        // Given
        Order order = createTestOrder();
        order.setOrderStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CancellationResponse response = bookingService.cancelBooking(1L, TEST_CUSTOMER_ID, null);

        // Then
        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getPreviousStatus());
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertNotNull(order.getCancelledAt());
        assertNull(order.getCancellationReason());
    }

    @Test
    void shouldThrowExceptionWhenCancellingCompletedOrder() {
        // Given
        Order order = createTestOrder();
        order.setOrderStatus(OrderStatus.COMPLETED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When / Then
        var exception = assertThrows(IllegalStateException.class, () ->
            bookingService.cancelBooking(1L, TEST_CUSTOMER_ID, "Test")
        );

        assertEquals("Cannot cancel completed order", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCancellingAlreadyCancelledOrder() {
        // Given
        Order order = createTestOrder();
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now().minusDays(1));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When / Then
        var exception = assertThrows(IllegalStateException.class, () ->
            bookingService.cancelBooking(1L, TEST_CUSTOMER_ID, "Test")
        );

        assertEquals("Order is already cancelled", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        var exception = assertThrows(IllegalArgumentException.class, () ->
            bookingService.cancelBooking(999L, TEST_CUSTOMER_ID, "Test")
        );

        assertTrue(exception.getMessage().contains("Order not found"));
    }

    @Test
    void shouldThrowExceptionWhenCustomerDoesNotOwnOrder() {
        // Given
        Order order = createTestOrder();
        Long wrongCustomerId = 999L;

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When / Then
        var exception = assertThrows(SecurityException.class, () ->
            bookingService.cancelBooking(1L, wrongCustomerId, "Test")
        );

        assertTrue(exception.getMessage().contains("not authorized"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldSendCancellationNotifications() {
        // Given
        Order order = createTestOrder();
        order.setOrderStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailNotification.sendNotification(any(SimpleMailMessage.class))).thenReturn(true);
        when(smsNotification.sendNotification(anyString())).thenReturn(true);

        // When
        bookingService.cancelBooking(1L, TEST_CUSTOMER_ID, "Changed plans");

        // Then
        verify(emailNotification).sendNotification(any(SimpleMailMessage.class));
        verify(smsNotification).sendNotification(anyString());
    }

    @Test
    void shouldNotFailCancellationWhenNotificationFails() throws Exception {
        // Given
        Order order = createTestOrder();
        order.setOrderStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationServiceFactory.createNotificationService(any())).thenThrow(new RuntimeException("Notification service down"));

        // When
        CancellationResponse response = bookingService.cancelBooking(1L, TEST_CUSTOMER_ID, "Test");

        // Then - Cancellation should succeed despite notification failure
        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldUnregisterOrderFromPriceObserver() {
        // Given
        Order order = createTestOrder();
        order.setOrderStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        bookingService.cancelBooking(1L, TEST_CUSTOMER_ID, null);

        // Then
        Long roomTypeId = order.getRoom().getRoomType().getId();
        verify(roomService).removeObserver(eq(roomTypeId), any(Order.class));
    }

    @Test
    void shouldNotFailCancellationWhenObserverCleanupFails() {
        // Given
        Order order = createTestOrder();
        order.setOrderStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Observer cleanup failed")).when(roomService).removeObserver(any(), any());

        // When
        CancellationResponse response = bookingService.cancelBooking(1L, TEST_CUSTOMER_ID, "Test");

        // Then - Cancellation should succeed despite observer cleanup failure
        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
    }
}
