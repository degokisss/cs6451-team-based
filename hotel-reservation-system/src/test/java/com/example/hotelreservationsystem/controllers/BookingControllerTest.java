package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.BookingCreateRequest;
import com.example.hotelreservationsystem.dto.BookingResponse;
import com.example.hotelreservationsystem.dto.CancellationRequest;
import com.example.hotelreservationsystem.dto.CancellationResponse;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.enums.OrderStatus;
import com.example.hotelreservationsystem.service.AuthenticationService;
import com.example.hotelreservationsystem.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private AuthenticationService authenticationService;

    private Customer mockCustomer;
    private static final Long TEST_CUSTOMER_ID = 100L;
    private static final Long TEST_ROOM_ID = 1L;
    private static final String TEST_LOCK_ID = "test-lock-123";

    @BeforeEach
    void setUp() {
        // Mock authenticated user
        mockCustomer = new Customer();
        mockCustomer.setId(TEST_CUSTOMER_ID);
        mockCustomer.setEmail("user@test.com");
        mockCustomer.setName("Test User");

        // Mock authentication service to return the mock customer
        when(authenticationService.getCurrentUser(anyString())).thenReturn(mockCustomer);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldCreateBookingSuccessfully() throws Exception {
        // Given
        var request = createValidRequest();
        var response = createBookingResponse();

        when(bookingService.createBooking(any(BookingCreateRequest.class))).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.customerId").value(TEST_CUSTOMER_ID))
            .andExpect(jsonPath("$.roomId").value(TEST_ROOM_ID))
            .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.totalPrice").value(300.00))
            .andExpect(jsonPath("$.checkInDate").exists())
            .andExpect(jsonPath("$.checkOutDate").exists())
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.checkInCode").exists())
            .andExpect(jsonPath("$.checkInCode").isString());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn400WhenLockIsInvalid() throws Exception {
        // Given
        var request = createValidRequest();

        when(bookingService.createBooking(any(BookingCreateRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid or expired lock"));

        // When / Then
        mockMvc.perform(post("/api/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Invalid or expired lock"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn403WhenLockBelongsToAnotherCustomer() throws Exception {
        // Given
        var request = createValidRequest();

        when(bookingService.createBooking(any(BookingCreateRequest.class)))
            .thenThrow(new SecurityException("Lock belongs to another customer"));

        // When / Then
        mockMvc.perform(post("/api/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(content().string("Lock belongs to another customer"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn403WhenCustomerIdMismatch() throws Exception {
        // Given - Request with different customer ID
        var request = createValidRequest();
        request.setCustomerId(999L); // Different from authenticated user

        // When / Then
        mockMvc.perform(post("/api/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(content().string("Customer ID does not match authenticated user"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn400WhenRoomNotFound() throws Exception {
        // Given
        var request = createValidRequest();

        when(bookingService.createBooking(any(BookingCreateRequest.class)))
            .thenThrow(new IllegalStateException("Room not found: " + TEST_ROOM_ID));

        // When / Then
        mockMvc.perform(post("/api/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn500OnUnexpectedError() throws Exception {
        // Given
        var request = createValidRequest();

        when(bookingService.createBooking(any(BookingCreateRequest.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When / Then
        mockMvc.perform(post("/api/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("Failed to create booking"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldValidateRequestBody() throws Exception {
        // Given - Invalid request with missing fields
        var invalidRequest = "{}";

        // When / Then
        mockMvc.perform(post("/api/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldValidateEmailFormat() throws Exception {
        // Given
        var request = createValidRequest();
        request.setGuestEmail("invalid-email"); // Invalid email format

        // When / Then
        mockMvc.perform(post("/api/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAuthenticationForBookingCreation() throws Exception {
        // Given - No authentication
        var request = createValidRequest();

        // When / Then
        mockMvc.perform(post("/api/bookings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    // Helper methods
    private BookingCreateRequest createValidRequest() {
        return BookingCreateRequest.builder()
            .lockId(TEST_LOCK_ID)
            .roomId(TEST_ROOM_ID)
            .customerId(TEST_CUSTOMER_ID)
            .guestName("Test Guest")
            .guestEmail("guest@example.com")
            .guestPhone("1234567890")
            .checkInDate(LocalDate.now().plusDays(7))
            .checkOutDate(LocalDate.now().plusDays(10))
            .build();
    }

    private BookingResponse createBookingResponse() {
        return BookingResponse.builder()
            .orderId(1L)
            .customerId(TEST_CUSTOMER_ID)
            .roomId(TEST_ROOM_ID)
            .orderStatus(OrderStatus.CONFIRMED)
            .totalPrice(new BigDecimal("300.00"))
            .checkInDate(LocalDate.now().plusDays(7))
            .checkOutDate(LocalDate.now().plusDays(10))
            .createdAt(LocalDateTime.now())
            .checkInCode("ABCD1234")
            .build();
    }

    // ============================================
    // Cancellation Tests
    // ============================================

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldCancelBookingSuccessfully() throws Exception {
        // Given
        Long orderId = 1L;
        var cancellationRequest = CancellationRequest.builder()
            .reason("Plans changed")
            .build();

        var cancellationResponse = CancellationResponse.builder()
            .orderId(orderId)
            .previousStatus(OrderStatus.CONFIRMED)
            .cancelledAt(LocalDateTime.now())
            .cancellationReason("Plans changed")
            .message("Booking cancelled successfully")
            .build();

        when(bookingService.cancelBooking(eq(orderId), eq(TEST_CUSTOMER_ID), eq("Plans changed")))
            .thenReturn(cancellationResponse);

        // When / Then
        mockMvc.perform(delete("/api/bookings/{orderId}", orderId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancellationRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.previousStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.cancelledAt").exists())
            .andExpect(jsonPath("$.cancellationReason").value("Plans changed"))
            .andExpect(jsonPath("$.message").value("Booking cancelled successfully"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldCancelBookingWithoutReason() throws Exception {
        // Given
        Long orderId = 1L;

        var cancellationResponse = CancellationResponse.builder()
            .orderId(orderId)
            .previousStatus(OrderStatus.PENDING)
            .cancelledAt(LocalDateTime.now())
            .cancellationReason(null)
            .message("Booking cancelled successfully")
            .build();

        when(bookingService.cancelBooking(eq(orderId), eq(TEST_CUSTOMER_ID), isNull()))
            .thenReturn(cancellationResponse);

        // When / Then - No request body (reason is optional)
        mockMvc.perform(delete("/api/bookings/{orderId}", orderId)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.previousStatus").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn404WhenOrderNotFoundForCancellation() throws Exception {
        // Given
        Long orderId = 999L;

        when(bookingService.cancelBooking(eq(orderId), eq(TEST_CUSTOMER_ID), any()))
            .thenThrow(new IllegalArgumentException("Order not found with ID: " + orderId));

        // When / Then
        mockMvc.perform(delete("/api/bookings/{orderId}", orderId)
                .with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(content().string("Order not found with ID: " + orderId));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn403WhenCancellingOtherUsersOrder() throws Exception {
        // Given
        Long orderId = 1L;

        when(bookingService.cancelBooking(eq(orderId), eq(TEST_CUSTOMER_ID), any()))
            .thenThrow(new SecurityException("You are not authorized to cancel this order"));

        // When / Then
        mockMvc.perform(delete("/api/bookings/{orderId}", orderId)
                .with(csrf()))
            .andExpect(status().isForbidden())
            .andExpect(content().string("You are not authorized to cancel this order"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn400WhenCancellingCompletedOrder() throws Exception {
        // Given
        Long orderId = 1L;

        when(bookingService.cancelBooking(eq(orderId), eq(TEST_CUSTOMER_ID), any()))
            .thenThrow(new IllegalStateException("Cannot cancel completed order"));

        // When / Then
        mockMvc.perform(delete("/api/bookings/{orderId}", orderId)
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Cannot cancel completed order"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn400WhenCancellingAlreadyCancelledOrder() throws Exception {
        // Given
        Long orderId = 1L;

        when(bookingService.cancelBooking(eq(orderId), eq(TEST_CUSTOMER_ID), any()))
            .thenThrow(new IllegalStateException("Order is already cancelled"));

        // When / Then
        mockMvc.perform(delete("/api/bookings/{orderId}", orderId)
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Order is already cancelled"));
    }

    @Test
    void shouldRequireAuthenticationForCancellation() throws Exception {
        // Given - No authentication
        Long orderId = 1L;

        // When / Then
        mockMvc.perform(delete("/api/bookings/{orderId}", orderId)
                .with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn500OnUnexpectedCancellationError() throws Exception {
        // Given
        Long orderId = 1L;

        when(bookingService.cancelBooking(eq(orderId), eq(TEST_CUSTOMER_ID), any()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When / Then
        mockMvc.perform(delete("/api/bookings/{orderId}", orderId)
                .with(csrf()))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("Failed to cancel booking"));
    }
}
